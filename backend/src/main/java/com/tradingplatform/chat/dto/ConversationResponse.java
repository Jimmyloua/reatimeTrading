package com.tradingplatform.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for conversation summary in list view.
 * Implements D-03: Item context shown as clickable link in chat header.
 * Implements D-17: Conversations ordered by most recent message.
 * Implements D-18: Unread message count shown on conversation list items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    private Long id;

    /**
     * ID of the listing this conversation is about.
     */
    private Long listingId;

    /**
     * Title of the listing for display.
     * Implements D-03: item context shown as clickable link.
     */
    private String listingTitle;

    /**
     * ID of the other participant in the conversation.
     */
    private Long otherUserId;

    /**
     * Display name of the other participant.
     */
    private String otherUserName;

    /**
     * Avatar URL of the other participant.
     */
    private String otherUserAvatar;

    /**
     * Preview of the last message.
     */
    private String lastMessage;

    /**
     * Timestamp of the last message.
     * Implements D-17: ordering by most recent message.
     */
    private LocalDateTime lastMessageAt;

    /**
     * Number of unread messages for the current user.
     * Implements D-18: unread count shown on conversation list.
     */
    private Integer unreadCount;

    /**
     * When the conversation was created.
     */
    private LocalDateTime createdAt;
}