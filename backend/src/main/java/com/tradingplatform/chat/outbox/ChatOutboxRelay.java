package com.tradingplatform.chat.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.chat.entity.ChatMessageOutbox;
import com.tradingplatform.chat.kafka.ChatDeliveryEvent;
import com.tradingplatform.chat.repository.ChatMessageOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOutboxRelay {

    public static final String TOPIC = "chat.message.persisted";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final ChatMessageOutboxRepository chatMessageOutboxRepository;
    private final KafkaTemplate<Long, ChatDeliveryEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public int relayPendingMessages() {
        List<ChatMessageOutbox> pendingRows = chatMessageOutboxRepository
            .findTop100ByStatusOrderByCreatedAtAsc(ChatMessageOutbox.STATUS_PENDING);

        for (ChatMessageOutbox pendingRow : pendingRows) {
            relayRow(pendingRow);
        }

        return pendingRows.size();
    }

    private void relayRow(ChatMessageOutbox outboxRow) {
        ChatDeliveryEvent deliveryEvent = toDeliveryEvent(outboxRow);

        try {
            kafkaTemplate.send(TOPIC, deliveryEvent.getConversationId(), deliveryEvent).join();
            outboxRow.setStatus(STATUS_PUBLISHED);
            outboxRow.setPublishedAt(LocalDateTime.now());
            chatMessageOutboxRepository.save(outboxRow);
        } catch (RuntimeException exception) {
            outboxRow.setStatus(STATUS_FAILED);
            outboxRow.setAttemptCount(outboxRow.getAttemptCount() + 1);
            chatMessageOutboxRepository.save(outboxRow);
            throw new IllegalStateException("Failed to relay chat outbox row " + outboxRow.getId(), exception);
        }
    }

    private ChatDeliveryEvent toDeliveryEvent(ChatMessageOutbox outboxRow) {
        try {
            return objectMapper.readValue(outboxRow.getPayload(), ChatDeliveryEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize chat outbox row " + outboxRow.getId(), exception);
        }
    }
}
