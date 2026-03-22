package com.tradingplatform.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction entity representing a peer-to-peer transaction.
 * Implements D-19 to D-25: Core transaction data capture.
 * Implements D-42: Single record with embedded LedgerEntries.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_trans_listing", columnList = "listing_id"),
    @Index(name = "idx_trans_buyer", columnList = "buyer_id"),
    @Index(name = "idx_trans_seller", columnList = "seller_id"),
    @Index(name = "idx_trans_status", columnList = "status"),
    @Index(name = "idx_trans_idempotency", columnList = "idempotency_key", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // D-19: Core identifiers
    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // D-24: Communication context (optional)
    @Column(name = "conversation_id")
    private Long conversationId;

    // D-20: Commercial data
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    // D-05: Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.CREATED;

    // D-22: Lifecycle timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "funded_at")
    private LocalDateTime fundedAt;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    // D-21: Idempotency for duplicate prevention
    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    // D-22: Cancellation
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    // D-44: Optimistic locking
    @Version
    private Long version;

    // D-42: Embedded ledger entries for atomicity
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    // Helper methods

    /**
     * Returns true if the given user is a participant (buyer or seller).
     *
     * @param userId the user ID to check
     * @return true if the user is a participant
     */
    public boolean isParticipant(Long userId) {
        return buyerId.equals(userId) || sellerId.equals(userId);
    }

    /**
     * Returns the role of the user in this transaction.
     *
     * @param userId the user ID
     * @return "BUYER", "SELLER", or null if not a participant
     */
    public String getRoleForUser(Long userId) {
        if (buyerId.equals(userId)) return "BUYER";
        if (sellerId.equals(userId)) return "SELLER";
        return null;
    }

    /**
     * Returns the other participant's ID.
     *
     * @param userId the current user's ID
     * @return the other participant's ID
     */
    public Long getOtherParticipantId(Long userId) {
        return buyerId.equals(userId) ? sellerId : buyerId;
    }
}