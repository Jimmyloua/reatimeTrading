package com.tradingplatform.notification.entity;

/**
 * Enum for notification types.
 * Per D-14: NEW_MESSAGE, ITEM_SOLD, TRANSACTION_UPDATE, SYSTEM_ANNOUNCEMENT, PAYMENT_STATUS.
 */
public enum NotificationType {
    NEW_MESSAGE,        // NOTF-01
    ITEM_SOLD,          // NOTF-02
    TRANSACTION_UPDATE, // D-14
    SYSTEM_ANNOUNCEMENT, // D-14
    PAYMENT_STATUS      // D-14
}