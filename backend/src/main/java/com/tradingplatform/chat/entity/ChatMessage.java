package com.tradingplatform.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ChatMessage entity representing an individual message in a conversation.
 * Implements D-06 from CONTEXT.md: Read receipts (sent, delivered, read).
 * Implements D-07 from CONTEXT.md: Image sharing supported in chat.
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_msg_conversation", columnList = "conversation_id"),
    @Index(name = "idx_msg_sender", columnList = "sender_id"),
    @Index(name = "idx_msg_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK to Conversation.
     */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /**
     * FK to User who sent the message.
     */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /**
     * Message text content.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Optional image URL for image sharing.
     * Implements D-07: image sharing supported in chat.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Message status for read receipts.
     * Implements D-06: Users see read receipts.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}