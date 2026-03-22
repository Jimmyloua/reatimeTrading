package com.tradingplatform.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger entry for double-entry bookkeeping.
 * Implements D-42: Embedded in Transaction for atomicity.
 * Implements D-45: Balances derived from ledger entries.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_transaction", columnList = "transaction_id"),
    @Index(name = "idx_ledger_idempotency", columnList = "idempotency_key", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    // Account types for double-entry bookkeeping
    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;  // BUYER_ESCROW, SELLER_PENDING, PLATFORM_FEE

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;  // DEBIT, CREDIT

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Account types for ledger entries.
     */
    public static final String ACCOUNT_BUYER_ESCROW = "BUYER_ESCROW";
    public static final String ACCOUNT_SELLER_PENDING = "SELLER_PENDING";
    public static final String ACCOUNT_PLATFORM_FEE = "PLATFORM_FEE";

    /**
     * Entry types.
     */
    public static final String TYPE_DEBIT = "DEBIT";
    public static final String TYPE_CREDIT = "CREDIT";
}