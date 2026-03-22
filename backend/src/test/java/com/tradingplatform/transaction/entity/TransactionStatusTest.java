package com.tradingplatform.transaction.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TransactionStatus enum state machine validation.
 * Implements D-05: Full lifecycle states
 * Implements D-06: Failure states
 * Implements D-08: Each transition requires explicit action
 */
class TransactionStatusTest {

    // Test 1: CREATED can transition to FUNDED, CANCELLED, EXPIRED
    @Test
    @DisplayName("CREATED can transition to FUNDED, CANCELLED, EXPIRED")
    void createdTransitions() {
        assertTrue(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.FUNDED));
        assertTrue(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.CANCELLED));
        assertTrue(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.EXPIRED));

        // Cannot transition to other states
        assertFalse(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.RESERVED));
        assertFalse(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.DELIVERED));
        assertFalse(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.CONFIRMED));
        assertFalse(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.SETTLED));
        assertFalse(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.COMPLETED));
        assertFalse(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.DISPUTED));
        assertFalse(TransactionStatus.CREATED.canTransitionTo(TransactionStatus.REFUNDED));
    }

    // Test 2: FUNDED can transition to RESERVED, CANCELLED, DISPUTED
    @Test
    @DisplayName("FUNDED can transition to RESERVED, CANCELLED, DISPUTED")
    void fundedTransitions() {
        assertTrue(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.RESERVED));
        assertTrue(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.CANCELLED));
        assertTrue(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.DISPUTED));

        // Cannot transition to other states
        assertFalse(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.CREATED));
        assertFalse(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.DELIVERED));
        assertFalse(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.CONFIRMED));
        assertFalse(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.SETTLED));
        assertFalse(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.COMPLETED));
        assertFalse(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.EXPIRED));
        assertFalse(TransactionStatus.FUNDED.canTransitionTo(TransactionStatus.REFUNDED));
    }

    // Test 3: RESERVED can transition to DELIVERED, DISPUTED
    @Test
    @DisplayName("RESERVED can transition to DELIVERED, DISPUTED")
    void reservedTransitions() {
        assertTrue(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.DELIVERED));
        assertTrue(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.DISPUTED));

        // Cannot transition to other states
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.CREATED));
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.FUNDED));
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.CONFIRMED));
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.SETTLED));
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.COMPLETED));
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.CANCELLED));
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.EXPIRED));
        assertFalse(TransactionStatus.RESERVED.canTransitionTo(TransactionStatus.REFUNDED));
    }

    // Test 4: DELIVERED can transition to CONFIRMED, DISPUTED
    @Test
    @DisplayName("DELIVERED can transition to CONFIRMED, DISPUTED")
    void deliveredTransitions() {
        assertTrue(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.CONFIRMED));
        assertTrue(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.DISPUTED));

        // Cannot transition to other states
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.CREATED));
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.FUNDED));
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.RESERVED));
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.SETTLED));
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.COMPLETED));
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.CANCELLED));
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.EXPIRED));
        assertFalse(TransactionStatus.DELIVERED.canTransitionTo(TransactionStatus.REFUNDED));
    }

    // Test 5: CONFIRMED can transition to SETTLED, DISPUTED
    @Test
    @DisplayName("CONFIRMED can transition to SETTLED, DISPUTED")
    void confirmedTransitions() {
        assertTrue(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.SETTLED));
        assertTrue(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.DISPUTED));

        // Cannot transition to other states
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.CREATED));
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.FUNDED));
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.RESERVED));
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.DELIVERED));
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.COMPLETED));
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.CANCELLED));
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.EXPIRED));
        assertFalse(TransactionStatus.CONFIRMED.canTransitionTo(TransactionStatus.REFUNDED));
    }

    // Test 6: SETTLED can transition to COMPLETED
    @Test
    @DisplayName("SETTLED can transition to COMPLETED")
    void settledTransitions() {
        assertTrue(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.COMPLETED));

        // Cannot transition to other states
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.CREATED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.FUNDED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.RESERVED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.DELIVERED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.CONFIRMED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.CANCELLED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.EXPIRED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.DISPUTED));
        assertFalse(TransactionStatus.SETTLED.canTransitionTo(TransactionStatus.REFUNDED));
    }

    // Test 7: Terminal states (COMPLETED, CANCELLED, EXPIRED, REFUNDED) have no valid transitions
    @Test
    @DisplayName("Terminal states have no valid transitions")
    void terminalStatesNoTransitions() {
        // COMPLETED - terminal
        for (TransactionStatus status : TransactionStatus.values()) {
            assertFalse(TransactionStatus.COMPLETED.canTransitionTo(status));
        }

        // CANCELLED - terminal
        for (TransactionStatus status : TransactionStatus.values()) {
            assertFalse(TransactionStatus.CANCELLED.canTransitionTo(status));
        }

        // EXPIRED - terminal
        for (TransactionStatus status : TransactionStatus.values()) {
            assertFalse(TransactionStatus.EXPIRED.canTransitionTo(status));
        }

        // REFUNDED - terminal
        for (TransactionStatus status : TransactionStatus.values()) {
            assertFalse(TransactionStatus.REFUNDED.canTransitionTo(status));
        }
    }

    // Additional tests for helper methods

    @Test
    @DisplayName("isTerminal returns true for terminal states")
    void isTerminal() {
        assertTrue(TransactionStatus.COMPLETED.isTerminal());
        assertTrue(TransactionStatus.CANCELLED.isTerminal());
        assertTrue(TransactionStatus.EXPIRED.isTerminal());
        assertTrue(TransactionStatus.REFUNDED.isTerminal());

        assertFalse(TransactionStatus.CREATED.isTerminal());
        assertFalse(TransactionStatus.FUNDED.isTerminal());
        assertFalse(TransactionStatus.RESERVED.isTerminal());
        assertFalse(TransactionStatus.DELIVERED.isTerminal());
        assertFalse(TransactionStatus.CONFIRMED.isTerminal());
        assertFalse(TransactionStatus.SETTLED.isTerminal());
        assertFalse(TransactionStatus.DISPUTED.isTerminal());
    }

    @Test
    @DisplayName("allowsRating returns true only for SETTLED and COMPLETED")
    void allowsRating() {
        assertTrue(TransactionStatus.SETTLED.allowsRating());
        assertTrue(TransactionStatus.COMPLETED.allowsRating());

        assertFalse(TransactionStatus.CREATED.allowsRating());
        assertFalse(TransactionStatus.FUNDED.allowsRating());
        assertFalse(TransactionStatus.RESERVED.allowsRating());
        assertFalse(TransactionStatus.DELIVERED.allowsRating());
        assertFalse(TransactionStatus.CONFIRMED.allowsRating());
        assertFalse(TransactionStatus.CANCELLED.allowsRating());
        assertFalse(TransactionStatus.EXPIRED.allowsRating());
        assertFalse(TransactionStatus.DISPUTED.allowsRating());
        assertFalse(TransactionStatus.REFUNDED.allowsRating());
    }

    @Test
    @DisplayName("isCancelable returns true only for CREATED")
    void isCancelable() {
        assertTrue(TransactionStatus.CREATED.isCancelable());

        assertFalse(TransactionStatus.FUNDED.isCancelable());
        assertFalse(TransactionStatus.RESERVED.isCancelable());
        assertFalse(TransactionStatus.DELIVERED.isCancelable());
        assertFalse(TransactionStatus.CONFIRMED.isCancelable());
        assertFalse(TransactionStatus.SETTLED.isCancelable());
        assertFalse(TransactionStatus.COMPLETED.isCancelable());
        assertFalse(TransactionStatus.CANCELLED.isCancelable());
        assertFalse(TransactionStatus.EXPIRED.isCancelable());
        assertFalse(TransactionStatus.DISPUTED.isCancelable());
        assertFalse(TransactionStatus.REFUNDED.isCancelable());
    }

    @Test
    @DisplayName("DISPUTED can transition to REFUNDED")
    void disputedTransitions() {
        assertTrue(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.REFUNDED));

        // Cannot transition to other states
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.CREATED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.FUNDED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.RESERVED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.DELIVERED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.CONFIRMED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.SETTLED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.COMPLETED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.CANCELLED));
        assertFalse(TransactionStatus.DISPUTED.canTransitionTo(TransactionStatus.EXPIRED));
    }
}