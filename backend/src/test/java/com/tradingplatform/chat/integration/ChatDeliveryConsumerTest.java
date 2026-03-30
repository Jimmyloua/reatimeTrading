package com.tradingplatform.chat.integration;

import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.kafka.ChatDeliveryConsumer;
import com.tradingplatform.chat.kafka.ChatDeliveryEvent;
import com.tradingplatform.chat.service.ChatDeliveryStatusService;
import com.tradingplatform.chat.service.ChatQueryService;
import com.tradingplatform.notification.service.NotificationPushService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatDeliveryConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChatDeliveryStatusService chatDeliveryStatusService;

    @Mock
    private ChatQueryService chatQueryService;

    @Mock
    private NotificationPushService notificationPushService;

    @InjectMocks
    private ChatDeliveryConsumer chatDeliveryConsumer;

    @Test
    @DisplayName("handleDelivery pushes recipient message and marks status delivered")
    void handleDeliveryPushesRecipientMessage() {
        ChatDeliveryEvent event = ChatDeliveryEvent.builder()
            .messageId(10L)
            .conversationId(55L)
            .recipientUserId(9L)
            .status(MessageStatus.PERSISTED)
            .createdAt(LocalDateTime.of(2026, 3, 30, 23, 40))
            .build();
        MessageResponse message = MessageResponse.builder()
            .id(10L)
            .conversationId(55L)
            .senderId(1L)
            .senderName("Buyer")
            .content("hello")
            .status(MessageStatus.DELIVERED)
            .createdAt(event.getCreatedAt())
            .isOwnMessage(false)
            .build();

        when(chatDeliveryStatusService.markDelivered(10L)).thenReturn(true);
        when(chatQueryService.getMessageForParticipant(55L, 10L, 9L)).thenReturn(message);

        chatDeliveryConsumer.handleDelivery(event);

        verify(messagingTemplate).convertAndSendToUser("9", "/queue/messages", message);
        verify(notificationPushService).pushMessageNotification(9L, message);
    }

    @Test
    @DisplayName("handleDelivery is idempotent when message was already delivered")
    void handleDeliveryIsIdempotent() {
        ChatDeliveryEvent event = ChatDeliveryEvent.builder()
            .messageId(10L)
            .conversationId(55L)
            .recipientUserId(9L)
            .status(MessageStatus.PERSISTED)
            .build();

        when(chatDeliveryStatusService.markDelivered(10L)).thenReturn(false);

        chatDeliveryConsumer.handleDelivery(event);

        verify(chatQueryService, never()).getMessageForParticipant(any(), any(), any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
        verify(notificationPushService, never()).pushMessageNotification(any(), any());
    }
}
