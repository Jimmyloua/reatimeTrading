package com.tradingplatform.transaction.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DisputeStatus enum and Dispute entity.
 * Implements D-15 to D-18: Admin-mediated dispute resolution.
 */
class DisputeStatusTest {

    // Test 1: DisputeStatus has all D-18 states
    @Test
    @DisplayName("DisputeStatus has all D-18 states")
    void disputeStatusHasAllStates() {
        // Per D-18: Dispute statuses for admin-mediated resolution
        assertNotNull(DisputeStatus.valueOf("OPEN"));
        assertNotNull(DisputeStatus.valueOf("UNDER_REVIEW"));
        assertNotNull(DisputeStatus.valueOf("WAITING_BUYER_EVIDENCE"));
        assertNotNull(DisputeStatus.valueOf("WAITING_SELLER_EVIDENCE"));
        assertNotNull(DisputeStatus.valueOf("RESOLVED_BUYER"));
        assertNotNull(DisputeStatus.valueOf("RESOLVED_SELLER"));
        assertNotNull(DisputeStatus.valueOf("PARTIALLY_RESOLVED"));
        assertNotNull(DisputeStatus.valueOf("ESCALATED"));
        assertNotNull(DisputeStatus.valueOf("CLOSED"));
    }

    // Test 2: isResolved returns true for resolved states
    @Test
    @DisplayName("isResolved returns true for resolved states")
    void isResolvedReturnsTrueForResolvedStates() {
        assertTrue(DisputeStatus.RESOLVED_BUYER.isResolved());
        assertTrue(DisputeStatus.RESOLVED_SELLER.isResolved());
        assertTrue(DisputeStatus.PARTIALLY_RESOLVED.isResolved());
        assertTrue(DisputeStatus.CLOSED.isResolved());

        assertFalse(DisputeStatus.OPEN.isResolved());
        assertFalse(DisputeStatus.UNDER_REVIEW.isResolved());
        assertFalse(DisputeStatus.WAITING_BUYER_EVIDENCE.isResolved());
        assertFalse(DisputeStatus.WAITING_SELLER_EVIDENCE.isResolved());
        assertFalse(DisputeStatus.ESCALATED.isResolved());
    }
}

/**
 * Tests for Dispute entity.
 */
class DisputeEntityTest {

    // Test 3: Dispute entity has required fields
    @Test
    @DisplayName("Dispute entity has transactionId, openerId, reason, description, status")
    void disputeEntityHasRequiredFields() {
        Dispute dispute = Dispute.builder()
            .id(1L)
            .transactionId(100L)
            .openerId(2L)
            .reason("Item not as described")
            .description("The item I received does not match the listing description.")
            .status(DisputeStatus.OPEN)
            .build();

        assertEquals(1L, dispute.getId());
        assertEquals(100L, dispute.getTransactionId());
        assertEquals(2L, dispute.getOpenerId());
        assertEquals("Item not as described", dispute.getReason());
        assertEquals("The item I received does not match the listing description.", dispute.getDescription());
        assertEquals(DisputeStatus.OPEN, dispute.getStatus());
    }

    // Test 4: Dispute entity has resolution field for admin decision
    @Test
    @DisplayName("Dispute entity has resolution field for admin decision")
    void disputeEntityHasResolutionField() {
        Dispute dispute = Dispute.builder()
            .id(1L)
            .transactionId(100L)
            .openerId(2L)
            .reason("Item not as described")
            .description("Test description")
            .status(DisputeStatus.RESOLVED_BUYER)
            .resolution("Full refund issued to buyer due to item mismatch.")
            .resolvedBy(999L)  // Admin ID
            .resolvedAt(LocalDateTime.now())
            .build();

        assertEquals("Full refund issued to buyer due to item mismatch.", dispute.getResolution());
        assertEquals(999L, dispute.getResolvedBy());
        assertNotNull(dispute.getResolvedAt());
    }

    // Test 5: Dispute has timestamps (createdAt, updatedAt, resolvedAt)
    @Test
    @DisplayName("Dispute has timestamps createdAt, updatedAt, resolvedAt")
    void disputeHasTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        Dispute dispute = Dispute.builder()
            .id(1L)
            .transactionId(100L)
            .openerId(2L)
            .reason("Test reason")
            .description("Test description")
            .createdAt(now)
            .updatedAt(now)
            .resolvedAt(now)
            .build();

        assertNotNull(dispute.getCreatedAt());
        assertNotNull(dispute.getUpdatedAt());
        assertNotNull(dispute.getResolvedAt());
    }

    // Test 6: isOpen returns true for open/modifiable states
    @Test
    @DisplayName("isOpen returns true for open/modifiable dispute states")
    void isOpenReturnsTrueForOpenStates() {
        Dispute openDispute = Dispute.builder()
            .transactionId(100L)
            .openerId(2L)
            .reason("Test")
            .description("Test")
            .status(DisputeStatus.OPEN)
            .build();

        Dispute underReviewDispute = Dispute.builder()
            .transactionId(100L)
            .openerId(2L)
            .reason("Test")
            .description("Test")
            .status(DisputeStatus.UNDER_REVIEW)
            .build();

        Dispute resolvedDispute = Dispute.builder()
            .transactionId(100L)
            .openerId(2L)
            .reason("Test")
            .description("Test")
            .status(DisputeStatus.RESOLVED_BUYER)
            .build();

        assertTrue(openDispute.isOpen());
        assertTrue(underReviewDispute.isOpen());
        assertFalse(resolvedDispute.isOpen());
    }

    // Test 7: Dispute default status is OPEN
    @Test
    @DisplayName("Dispute default status is OPEN")
    void disputeDefaultStatusIsOpen() {
        Dispute dispute = Dispute.builder()
            .transactionId(100L)
            .openerId(2L)
            .reason("Test")
            .description("Test")
            .build();

        assertEquals(DisputeStatus.OPEN, dispute.getStatus());
    }
}