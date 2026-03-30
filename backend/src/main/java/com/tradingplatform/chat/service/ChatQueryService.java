package com.tradingplatform.chat.service;

import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.chat.entity.ChatMessage;
import com.tradingplatform.chat.entity.Conversation;
import com.tradingplatform.chat.mapper.ChatMapper;
import com.tradingplatform.chat.repository.ChatMessageRepository;
import com.tradingplatform.chat.repository.ConversationRepository;
import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatQueryService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMapper chatMapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesAfter(Long conversationId, Long userId, Long afterMessageId) {
        Conversation conversation = requireParticipantConversation(conversationId, userId);
        return chatMessageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversation.getId(), afterMessageId)
            .stream()
            .map(message -> enrichMessage(message, userId))
            .toList();
    }

    @Transactional(readOnly = true)
    public MessageResponse getMessageForParticipant(Long conversationId, Long messageId, Long userId) {
        requireParticipantConversation(conversationId, userId);
        ChatMessage message = chatMessageRepository.findByIdAndConversationId(messageId, conversationId)
            .orElseThrow(() -> new IllegalStateException("Chat message not found: " + messageId));
        return enrichMessage(message, userId);
    }

    private Conversation requireParticipantConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND));
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw new ApiException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
        }
        return conversation;
    }

    private MessageResponse enrichMessage(ChatMessage message, Long currentUserId) {
        MessageResponse response = chatMapper.toMessageResponse(message, currentUserId);
        userRepository.findById(message.getSenderId())
            .ifPresent(user -> response.setSenderName(user.getDisplayNameOrFallback()));
        return response;
    }
}
