package com.tradingplatform.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.chat.dto.MessageAck;
import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.chat.entity.ChatMessage;
import com.tradingplatform.chat.entity.ChatMessageOutbox;
import com.tradingplatform.chat.entity.Conversation;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.mapper.ChatMapper;
import com.tradingplatform.chat.repository.ChatMessageOutboxRepository;
import com.tradingplatform.chat.repository.ConversationRepository;
import com.tradingplatform.chat.repository.MessageRepository;
import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageCommandService {

    private static final int MESSAGE_PREVIEW_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatMessageOutboxRepository chatMessageOutboxRepository;
    private final ChatMapper chatMapper;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PersistedChatMessage persistMessage(SendChatMessageCommand command) {
        Conversation conversation = loadConversation(command.conversationId());
        validateParticipant(conversation, command.senderId());

        ChatMessage message = saveMessage(command, conversation.getId());
        updateConversation(conversation, command.senderId(), command.content());

        Long recipientUserId = conversation.getOtherParticipantId(command.senderId());
        chatMessageOutboxRepository.save(buildOutbox(message, recipientUserId));

        return new PersistedChatMessage(
            enrichMessageResponse(message, command.senderId()),
            buildAck(message, command.clientMessageId()),
            recipientUserId
        );
    }

    private Conversation loadConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    private void validateParticipant(Conversation conversation, Long senderId) {
        if (!conversation.getBuyerId().equals(senderId) && !conversation.getSellerId().equals(senderId)) {
            throw new ApiException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
        }
    }

    private ChatMessage saveMessage(SendChatMessageCommand command, Long conversationId) {
        ChatMessage message = ChatMessage.builder()
            .conversationId(conversationId)
            .senderId(command.senderId())
            .content(command.content())
            .imageUrl(command.imageUrl())
            .status(MessageStatus.PERSISTED)
            .build();
        return messageRepository.save(message);
    }

    private void updateConversation(Conversation conversation, Long senderId, String content) {
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastMessagePreview(truncate(content));
        if (senderId.equals(conversation.getBuyerId())) {
            conversation.setSellerUnreadCount(conversation.getSellerUnreadCount() + 1);
        } else {
            conversation.setBuyerUnreadCount(conversation.getBuyerUnreadCount() + 1);
        }
        conversationRepository.save(conversation);
    }

    private ChatMessageOutbox buildOutbox(ChatMessage message, Long recipientUserId) {
        return ChatMessageOutbox.builder()
            .messageId(message.getId())
            .conversationId(message.getConversationId())
            .recipientUserId(recipientUserId)
            .eventType(ChatMessageOutbox.EVENT_TYPE_MESSAGE_PERSISTED)
            .payload(serializePayload(message, recipientUserId))
            .status(ChatMessageOutbox.STATUS_PENDING)
            .attemptCount(0)
            .createdAt(message.getCreatedAt())
            .build();
    }

    private String serializePayload(ChatMessage message, Long recipientUserId) {
        try {
            return objectMapper.writeValueAsString(new OutboxPayload(
                message.getId(),
                message.getConversationId(),
                recipientUserId,
                message.getSenderId(),
                message.getStatus(),
                message.getCreatedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chat message outbox payload", exception);
        }
    }

    private MessageResponse enrichMessageResponse(ChatMessage message, Long currentUserId) {
        MessageResponse response = chatMapper.toMessageResponse(message, currentUserId);
        userRepository.findById(message.getSenderId())
            .ifPresent(user -> response.setSenderName(user.getDisplayNameOrFallback()));
        return response;
    }

    private MessageAck buildAck(ChatMessage message, String clientMessageId) {
        return MessageAck.builder()
            .clientMessageId(clientMessageId)
            .messageId(message.getId())
            .conversationId(message.getConversationId())
            .status(message.getStatus())
            .createdAt(message.getCreatedAt())
            .build();
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= MESSAGE_PREVIEW_LENGTH
            ? content
            : content.substring(0, MESSAGE_PREVIEW_LENGTH);
    }

    public record SendChatMessageCommand(
        Long conversationId,
        Long senderId,
        String content,
        String imageUrl,
        String clientMessageId
    ) {
    }

    public record PersistedChatMessage(
        MessageResponse message,
        MessageAck ack,
        Long recipientUserId
    ) {
    }

    private record OutboxPayload(
        Long messageId,
        Long conversationId,
        Long recipientUserId,
        Long senderId,
        MessageStatus status,
        LocalDateTime createdAt
    ) {
    }
}
