package com.tradingplatform.chat.entity;

/**
 * Message status enum for read receipts.
 * Implements D-06 from CONTEXT.md: Users see read receipts (sent, delivered, read).
 */
public enum MessageStatus {
    /**
     * Message has been saved to the database.
     */
    SENT,

    /**
     * Message has been received by the recipient's device.
     */
    DELIVERED,

    /**
     * Recipient has opened and read the message.
     */
    READ
}