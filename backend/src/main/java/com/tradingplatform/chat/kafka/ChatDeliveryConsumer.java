package com.tradingplatform.chat.kafka;

import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.chat.outbox.ChatOutboxRelay;
import com.tradingplatform.chat.service.ChatDeliveryStatusService;
import com.tradingplatform.chat.service.ChatQueryService;
import com.tradingplatform.notification.service.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDeliveryConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatDeliveryStatusService chatDeliveryStatusService;
    private final ChatQueryService chatQueryService;
    private final NotificationPushService notificationPushService;

    @KafkaListener(topics = ChatOutboxRelay.TOPIC, groupId = "${chat.delivery.group-id:chat-delivery}")
    public void handleDelivery(ChatDeliveryEvent deliveryEvent) {
        boolean newlyDelivered = chatDeliveryStatusService.markDelivered(deliveryEvent.getMessageId());
        if (!newlyDelivered) {
            log.debug("Skipping duplicate chat delivery for message {}", deliveryEvent.getMessageId());
            return;
        }

        MessageResponse message = chatQueryService.getMessageForParticipant(
            deliveryEvent.getConversationId(),
            deliveryEvent.getMessageId(),
            deliveryEvent.getRecipientUserId()
        );

        messagingTemplate.convertAndSendToUser(
            deliveryEvent.getRecipientUserId().toString(),
            "/queue/messages",
            message
        );
        notificationPushService.pushMessageNotification(deliveryEvent.getRecipientUserId(), message);
    }
}
