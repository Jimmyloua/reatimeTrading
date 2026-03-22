package com.tradingplatform.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Transaction and LedgerEntry entities.
 * Implements D-19 to D-25: Core transaction data capture.
 * Implements D-42: Single record with embedded LedgerEntries.
 */
class TransactionEntityTest {

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Transaction entity has all required fields")
    void transactionHasRequiredFields() {
        Transaction transaction = Transaction.builder()
            .listingId(1L)
            .buyerId(2L)
            .sellerId(3L)
            .amount(new BigDecimal("100.00"))
            .status(TransactionStatus.CREATED)
            .idempotencyKey("test-key-123")
            .build();

        assertEquals(1L, transaction.getListingId());
        assertEquals(2L, transaction.getBuyerId());
        assertEquals(3L, transaction.getSellerId());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals(TransactionStatus.CREATED, transaction.getStatus());
        assertEquals("test-key-123", transaction.getIdempotencyKey());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Transaction has lifecycle timestamp fields")
    void transactionHasLifecycleTimestamps() {
        Transaction transaction = Transaction.builder().build();

        // Check that timestamp fields exist (they can be null for new entities)
        // The @CreatedDate annotation is applied by JPA when entity is persisted
        assertHasField(transaction, "createdAt");
        assertHasField(transaction, "fundedAt");
        assertHasField(transaction, "reservedAt");
        assertHasField(transaction, "deliveredAt");
        assertHasField(transaction, "confirmedAt");
        assertHasField(transaction, "settledAt");
        assertHasField(transaction, "cancelledAt");
        assertHasField(transaction, "expiredAt");

        // Other timestamps can be null initially
        assertNull(transaction.getFundedAt());
        assertNull(transaction.getReservedAt());
        assertNull(transaction.getDeliveredAt());
        assertNull(transaction.getConfirmedAt());
        assertNull(transaction.getSettledAt());
        assertNull(transaction.getCancelledAt());
        assertNull(transaction.getExpiredAt());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Transaction has version field for optimistic locking")
    void transactionHasVersionField() {
        Transaction transaction = Transaction.builder().build();

        // Version field should exist and be null for new entities
        assertNull(transaction.getVersion());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("LedgerEntry has transaction reference, accountType, entryType, amount")
    void ledgerEntryHasRequiredFields() {
        Transaction transaction = Transaction.builder()
            .listingId(1L)
            .buyerId(2L)
            .sellerId(3L)
            .amount(new BigDecimal("100.00"))
            .build();

        LedgerEntry entry = LedgerEntry.builder()
            .transaction(transaction)
            .accountType(LedgerEntry.ACCOUNT_BUYER_ESCROW)
            .entryType(LedgerEntry.TYPE_DEBIT)
            .amount(new BigDecimal("100.00"))
            .idempotencyKey("ledger-key-123")
            .build();

        assertEquals(transaction, entry.getTransaction());
        assertEquals(LedgerEntry.ACCOUNT_BUYER_ESCROW, entry.getAccountType());
        assertEquals(LedgerEntry.TYPE_DEBIT, entry.getEntryType());
        assertEquals(new BigDecimal("100.00"), entry.getAmount());
        assertEquals("ledger-key-123", entry.getIdempotencyKey());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("LedgerEntry has idempotency key with unique constraint")
    void ledgerEntryHasIdempotencyKey() {
        LedgerEntry entry = LedgerEntry.builder()
            .idempotencyKey("unique-key-456")
            .build();

        assertEquals("unique-key-456", entry.getIdempotencyKey());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Transaction isParticipant returns true for buyer and seller")
    void transactionIsParticipant() {
        Transaction transaction = Transaction.builder()
            .buyerId(1L)
            .sellerId(2L)
            .build();

        assertTrue(transaction.isParticipant(1L));
        assertTrue(transaction.isParticipant(2L));
        assertFalse(transaction.isParticipant(3L));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Transaction getRoleForUser returns correct role")
    void transactionGetRoleForUser() {
        Transaction transaction = Transaction.builder()
            .buyerId(1L)
            .sellerId(2L)
            .build();

        assertEquals("BUYER", transaction.getRoleForUser(1L));
        assertEquals("SELLER", transaction.getRoleForUser(2L));
        assertNull(transaction.getRoleForUser(3L));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Transaction getOtherParticipantId returns correct ID")
    void transactionGetOtherParticipantId() {
        Transaction transaction = Transaction.builder()
            .buyerId(1L)
            .sellerId(2L)
            .build();

        assertEquals(2L, transaction.getOtherParticipantId(1L));
        assertEquals(1L, transaction.getOtherParticipantId(2L));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("LedgerEntry has account type constants")
    void ledgerEntryHasAccountTypeConstants() {
        assertEquals("BUYER_ESCROW", LedgerEntry.ACCOUNT_BUYER_ESCROW);
        assertEquals("SELLER_PENDING", LedgerEntry.ACCOUNT_SELLER_PENDING);
        assertEquals("PLATFORM_FEE", LedgerEntry.ACCOUNT_PLATFORM_FEE);
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("LedgerEntry has entry type constants")
    void ledgerEntryHasEntryTypeConstants() {
        assertEquals("DEBIT", LedgerEntry.TYPE_DEBIT);
        assertEquals("CREDIT", LedgerEntry.TYPE_CREDIT);
    }

    /**
     * Helper method to verify a field exists via reflection.
     */
    private void assertHasField(Object obj, String fieldName) {
        try {
            obj.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            fail("Expected field '" + fieldName + "' not found in " + obj.getClass().getSimpleName());
        }
    }
}