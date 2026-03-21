package com.tradingplatform.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Conversation entity representing a chat thread between two users about a specific item.
 * Implements D-02 from CONTEXT.md: Each item creates a separate thread (not merged).
 * Implements D-17: Conversations ordered by most recent message.
 * Implements D-18: Unread message count shown on conversation list items.
 */
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conv_listing", columnList = "listing_id"),
    @Index(name = "idx_conv_buyer", columnList = "buyer_id"),
    @Index(name = "idx_conv_seller", columnList = "seller_id"),
    @Index(name = "idx_conv_last_msg", columnList = "last_message_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK to Listing - each item creates a separate conversation thread.
     * Implements D-02: separate thread per item.
     */
    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    /**
     * User who initiated the chat.
     */
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    /**
     * Owner of the listing.
     */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /**
     * Timestamp of the most recent message, used for ordering.
     * Implements D-17: conversations ordered by most recent message.
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * Preview of the last message (truncated to 100 chars).
     */
    @Column(name = "last_message_preview", length = 100)
    private String lastMessagePreview;

    /**
     * Unread count for the buyer.
     * Implements D-18: unread message count shown on conversation list items.
     */
    @Column(name = "buyer_unread_count")
    @Builder.Default
    private Integer buyerUnreadCount = 0;

    /**
     * Unread count for the seller.
     * Implements D-18: unread message count shown on conversation list items.
     */
    @Column(name = "seller_unread_count")
    @Builder.Default
    private Integer sellerUnreadCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Returns the ID of the other participant in the conversation.
     *
     * @param currentUserId the current user's ID
     * @return the other participant's ID
     */
    public Long getOtherParticipantId(Long currentUserId) {
        return currentUserId.equals(buyerId) ? sellerId : buyerId;
    }

    /**
     * Returns the unread count for a specific user.
     *
     * @param userId the user's ID
     * @return the unread count for that user
     */
    public Integer getUnreadCountForUser(Long userId) {
        return userId.equals(buyerId) ? buyerUnreadCount : sellerUnreadCount;
    }
}