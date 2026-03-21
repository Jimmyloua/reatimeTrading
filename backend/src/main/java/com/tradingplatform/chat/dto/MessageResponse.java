package com.tradingplatform.chat.dto;

import com.tradingplatform.chat.entity.MessageStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for a single chat message.
 * Implements D-06: Read receipts (sent, delivered, read).
 * Implements D-07: Image sharing supported in chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private Long id;

    /**
     * ID of the conversation this message belongs to.
     */
    private Long conversationId;

    /**
     * ID of the message sender.
     */
    private Long senderId;

    /**
     * Display name of the sender.
     */
    private String senderName;

    /**
     * Message text content.
     */
    private String content;

    /**
     * URL of attached image, if any.
     * Implements D-07: image sharing support.
     */
    private String imageUrl;

    /**
     * Current status of the message.
     * Implements D-06: read receipts.
     */
    private MessageStatus status;

    /**
     * When the message was created.
     */
    private LocalDateTime createdAt;

    /**
     * Whether this message was sent by the current user.
     */
    private boolean isOwnMessage;
}