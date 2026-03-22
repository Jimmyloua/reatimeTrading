package com.tradingplatform.transaction.entity;

/**
 * Dispute status enumeration.
 * Per D-18: Dispute statuses for admin-mediated resolution.
 */
public enum DisputeStatus {
    /**
     * Dispute opened, awaiting admin review.
     */
    OPEN,

    /**
     * Admin is reviewing the case.
     */
    UNDER_REVIEW,

    /**
     * Waiting for buyer to submit evidence.
     */
    WAITING_BUYER_EVIDENCE,

    /**
     * Waiting for seller to submit evidence.
     */
    WAITING_SELLER_EVIDENCE,

    /**
     * Resolved in buyer's favor (full refund).
     */
    RESOLVED_BUYER,

    /**
     * Resolved in seller's favor (funds released).
     */
    RESOLVED_SELLER,

    /**
     * Partial refund issued.
     */
    PARTIALLY_RESOLVED,

    /**
     * Escalated to higher authority.
     */
    ESCALATED,

    /**
     * Dispute closed.
     */
    CLOSED;

    /**
     * Returns true if this is a resolved state.
     *
     * @return true if resolved, false otherwise
     */
    public boolean isResolved() {
        return this == RESOLVED_BUYER || this == RESOLVED_SELLER
            || this == PARTIALLY_RESOLVED || this == CLOSED;
    }
}