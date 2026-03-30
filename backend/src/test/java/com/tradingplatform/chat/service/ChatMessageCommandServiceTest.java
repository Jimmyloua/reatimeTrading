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
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageCommandServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatMessageOutboxRepository chatMessageOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatMessageCommandService chatMessageCommandService;

    private Conversation conversation;
    private LocalDateTime createdAt;

    @BeforeEach
    void setUp() {
        createdAt = LocalDateTime.of(2026, 3, 30, 12, 0);
        conversation = Conversation.builder()
            .id(10L)
            .buyerId(1L)
            .sellerId(2L)
            .buyerUnreadCount(0)
            .sellerUnreadCount(0)
            .build();
    }

    @Test
    @DisplayName("persistMessage stores chat message and outbox row with persisted ack fields")
    void persistMessageStoresChatMessageAndOutboxRow() throws JsonProcessingException {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(99L);
            message.setCreatedAt(createdAt);
            return message;
        });
        when(chatMessageOutboxRepository.save(any(ChatMessageOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"messageId\":99}");
        when(chatMapper.toMessageResponse(any(ChatMessage.class), any(Long.class))).thenReturn(
            MessageResponse.builder()
                .id(99L)
                .conversationId(10L)
                .senderId(1L)
                .content("hello")
                .status(MessageStatus.PERSISTED)
                .createdAt(createdAt)
                .build()
        );

        ChatMessageCommandService.PersistedChatMessage result = chatMessageCommandService.persistMessage(
            new ChatMessageCommandService.SendChatMessageCommand(10L, 1L, "hello", null, "client-123")
        );

        ArgumentCaptor<ChatMessageOutbox> outboxCaptor = ArgumentCaptor.forClass(ChatMessageOutbox.class);
        verify(chatMessageOutboxRepository).save(outboxCaptor.capture());

        ChatMessageOutbox outbox = outboxCaptor.getValue();
        MessageAck ack = result.ack();

        assertNotNull(result.message());
        assertEquals(99L, outbox.getMessageId());
        assertEquals(10L, outbox.getConversationId());
        assertEquals(2L, outbox.getRecipientUserId());
        assertEquals("MESSAGE_PERSISTED", outbox.getEventType());
        assertEquals("PENDING", outbox.getStatus());
        assertEquals(0, outbox.getAttemptCount());
        assertEquals("client-123", ack.getClientMessageId());
        assertEquals(99L, ack.getMessageId());
        assertEquals(10L, ack.getConversationId());
        assertEquals(MessageStatus.PERSISTED, ack.getStatus());
        assertEquals(createdAt, ack.getCreatedAt());
    }
}
