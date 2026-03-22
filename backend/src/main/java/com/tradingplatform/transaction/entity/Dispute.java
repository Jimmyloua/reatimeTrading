package com.tradingplatform.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Dispute entity for transaction conflict resolution.
 * Per D-15 to D-18: Admin-mediated dispute resolution.
 */
@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_disputes_transaction", columnList = "transaction_id"),
    @Index(name = "idx_disputes_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "opener_id", nullable = false)
    private Long openerId;

    /**
     * Reason for dispute.
     * Per UI-SPEC: "Item not as described", "Item not received", "Seller unresponsive", "Other"
     */
    @Column(nullable = false, length = 100)
    private String reason;

    /**
     * Detailed description of the issue.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Current dispute status.
     * Per D-18: Status workflow.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    /**
     * Admin's resolution decision.
     */
    @Column(columnDefinition = "TEXT")
    private String resolution;

    /**
     * Admin who resolved the dispute (optional, for audit).
     */
    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Returns true if dispute can be modified.
     *
     * @return true if the dispute is in an open/modifiable state
     */
    public boolean isOpen() {
        return status == DisputeStatus.OPEN || status == DisputeStatus.UNDER_REVIEW
            || status == DisputeStatus.WAITING_BUYER_EVIDENCE
            || status == DisputeStatus.WAITING_SELLER_EVIDENCE;
    }
}