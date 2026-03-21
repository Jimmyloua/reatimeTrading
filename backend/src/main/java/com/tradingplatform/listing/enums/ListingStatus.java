package com.tradingplatform.listing.enums;

/**
 * Enumeration for listing status in the marketplace.
 */
public enum ListingStatus {
    /**
     * Listing is active and visible to buyers.
     */
    AVAILABLE,

    /**
     * Listing is reserved, pending transaction completion.
     */
    RESERVED,

    /**
     * Item has been sold, listing is archived.
     */
    SOLD
}
