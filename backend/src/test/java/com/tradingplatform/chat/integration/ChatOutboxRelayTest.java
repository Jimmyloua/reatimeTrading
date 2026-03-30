package com.tradingplatform.chat.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.chat.entity.ChatMessageOutbox;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.kafka.ChatDeliveryEvent;
import com.tradingplatform.chat.outbox.ChatOutboxRelay;
import com.tradingplatform.chat.repository.ChatMessageOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatOutboxRelayTest {

    @Mock
    private ChatMessageOutboxRepository chatMessageOutboxRepository;

    @Mock
    private KafkaTemplate<Long, ChatDeliveryEvent> kafkaTemplate;

    @Test
    @DisplayName("relayPendingMessages publishes conversation keyed Kafka events and marks rows published")
    void relayPendingMessagesPublishesConversationKeyedEvents() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChatOutboxRelay relay = new ChatOutboxRelay(chatMessageOutboxRepository, kafkaTemplate, objectMapper);

        ChatMessageOutbox outboxRow = ChatMessageOutbox.builder()
            .id(1L)
            .messageId(10L)
            .conversationId(55L)
            .recipientUserId(9L)
            .payload(objectMapper.writeValueAsString(ChatDeliveryEvent.builder()
                .messageId(10L)
                .conversationId(55L)
                .recipientUserId(9L)
                .senderId(1L)
                .status(MessageStatus.PERSISTED)
                .createdAt(LocalDateTime.of(2026, 3, 30, 23, 30))
                .build()))
            .status(ChatMessageOutbox.STATUS_PENDING)
            .attemptCount(0)
            .createdAt(LocalDateTime.of(2026, 3, 30, 23, 30))
            .build();

        when(chatMessageOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(ChatMessageOutbox.STATUS_PENDING))
            .thenReturn(List.of(outboxRow));
        when(kafkaTemplate.send(eq(ChatOutboxRelay.TOPIC), eq(55L), any(ChatDeliveryEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        relay.relayPendingMessages();

        ArgumentCaptor<ChatDeliveryEvent> eventCaptor = ArgumentCaptor.forClass(ChatDeliveryEvent.class);
        ArgumentCaptor<ChatMessageOutbox> outboxCaptor = ArgumentCaptor.forClass(ChatMessageOutbox.class);

        verify(kafkaTemplate).send(eq(ChatOutboxRelay.TOPIC), eq(55L), eventCaptor.capture());
        verify(chatMessageOutboxRepository).save(outboxCaptor.capture());

        assertEquals(55L, eventCaptor.getValue().getConversationId());
        assertEquals(10L, eventCaptor.getValue().getMessageId());
        assertEquals(9L, eventCaptor.getValue().getRecipientUserId());
        assertEquals("PUBLISHED", outboxCaptor.getValue().getStatus());
        assertNotNull(outboxCaptor.getValue().getPublishedAt());
    }
}
