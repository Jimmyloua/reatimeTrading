package com.tradingplatform.notification.service;

import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.notification.dto.NotificationResponse;
import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for pushing real-time notifications via WebSocket.
 * Implements NOTF-01: Real-time notification on message.
 * Implements NOTF-02: Notification when item sells.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final NotificationService notificationService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push notification when user receives a new message.
     * Implements NOTF-01.
     *
     * @param recipientId the recipient user ID
     * @param message the message that was received
     */
    public void pushMessageNotification(Long recipientId, MessageResponse message) {
        if (!notificationPreferenceService.isEnabled(recipientId, NotificationType.NEW_MESSAGE)) {
            log.debug("Skipping NEW_MESSAGE notification for user {} because the category is disabled", recipientId);
            return;
        }

        String title = "New Message";
        String content = message.getSenderName() + " sent you a message";

        Notification notification = notificationService.createNotification(
            recipientId,
            NotificationType.NEW_MESSAGE,
            title,
            content,
            message.getConversationId(),
            "conversation"
        );

        pushNotification(recipientId, notification);
    }

    /**
     * Push notification when item is sold.
     * Implements NOTF-02.
     *
     * @param sellerId the seller user ID
     * @param listingId the listing ID
     * @param listingTitle the listing title
     */
    public void pushItemSoldNotification(Long sellerId, Long listingId, String listingTitle) {
        if (!notificationPreferenceService.isEnabled(sellerId, NotificationType.ITEM_SOLD)) {
            log.debug("Skipping ITEM_SOLD notification for user {} because the category is disabled", sellerId);
            return;
        }

        String title = "Item Sold";
        String content = "Your item '" + truncate(listingTitle, 50) + "' was marked as sold";

        Notification notification = notificationService.createNotification(
            sellerId,
            NotificationType.ITEM_SOLD,
            title,
            content,
            listingId,
            "listing"
        );

        pushNotification(sellerId, notification);
    }

    /**
     * Push notification for transaction updates.
     *
     * @param userId the user ID
     * 
     * @param transactionId the transaction ID
     * @param status the transaction status
     */
    public void pushTransactionNotification(Long userId, Long transactionId, String status) {
        if (!notificationPreferenceService.isEnabled(userId, NotificationType.TRANSACTION_UPDATE)) {
            log.debug("Skipping TRANSACTION_UPDATE notification for user {} because the category is disabled", userId);
            return;
        }

        String title = "Transaction Update";
        String content = "Transaction status: " + status;

        Notification notification = notificationService.createNotification(
            userId,
            NotificationType.TRANSACTION_UPDATE,
            title,
            content,
            transactionId,
            "transaction"
        );

        pushNotification(userId, notification);
    }

    /**
     * Push a notification via WebSocket to a specific user.
     *
     * @param userId the user ID
     * @param notification the notification to push
     */
    private void pushNotification(Long userId, Notification notification) {
        NotificationResponse response = NotificationResponse.builder()
            .id(notification.getId())
            .type(notification.getType())
            .title(notification.getTitle())
            .content(notification.getContent())
            .referenceId(notification.getReferenceId())
            .referenceType(notification.getReferenceType())
            .read(notification.getRead())
            .createdAt(notification.getCreatedAt())
            .build();

        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            response
        );

        log.debug("Pushed notification {} to user {}", notification.getId(), userId);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
