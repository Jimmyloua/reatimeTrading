package com.tradingplatform.transaction.entity;

/**
 * Enumeration for transaction status with state machine validation.
 * Implements D-05: Full lifecycle states
 * Implements D-06: Failure states
 * Implements D-08: Each transition requires explicit action
 */
public enum TransactionStatus {
    // Lifecycle states (D-05)
    /**
     * Transaction initiated, awaiting seller acceptance.
     */
    CREATED,

    /**
     * Buyer confirmed payment sent.
     */
    FUNDED,

    /**
     * Seller confirmed funds received.
     */
    RESERVED,

    /**
     * Seller marked shipped/pickup ready.
     */
    DELIVERED,

    /**
     * Buyer confirmed receipt.
     */
    CONFIRMED,

    /**
     * Funds released (automatic after CONFIRMED).
     */
    SETTLED,

    /**
     * Transaction done, ratings enabled.
     */
    COMPLETED,

    // Failure states (D-06)
    /**
     * Cancelled before completion.
     */
    CANCELLED,

    /**
     * Auto-expired due to inactivity.
     */
    EXPIRED,

    /**
     * Under dispute.
     */
    DISPUTED,

    /**
     * Refund issued.
     */
    REFUNDED;

    /**
     * Validates if transition to target status is allowed.
     * Per D-08: Each transition requires explicit action.
     *
     * @param target the target status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(TransactionStatus target) {
        return switch (this) {
            case CREATED -> target == FUNDED || target == CANCELLED || target == EXPIRED;
            case FUNDED -> target == RESERVED || target == CANCELLED || target == DISPUTED;
            case RESERVED -> target == DELIVERED || target == DISPUTED;
            case DELIVERED -> target == CONFIRMED || target == DISPUTED;
            case CONFIRMED -> target == SETTLED || target == DISPUTED;
            case SETTLED -> target == COMPLETED;
            case DISPUTED -> target == REFUNDED;
            // Terminal states - no transitions allowed
            case COMPLETED, CANCELLED, EXPIRED, REFUNDED -> false;
        };
    }

    /**
     * Returns true if this is a terminal state (no further transitions).
     *
     * @return true if terminal state, false otherwise
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED || this == REFUNDED;
    }

    /**
     * Returns true if rating is allowed in this state.
     * Per D-29: Rating only available after SETTLED state.
     *
     * @return true if rating is allowed, false otherwise
     */
    public boolean allowsRating() {
        return this == SETTLED || this == COMPLETED;
    }

    /**
     * Returns true if cancellation is allowed by either party.
     * Per D-10: Cancelable before FUNDED state.
     *
     * @return true if cancelable, false otherwise
     */
    public boolean isCancelable() {
        return this == CREATED;
    }
}