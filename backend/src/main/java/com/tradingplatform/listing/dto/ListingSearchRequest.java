package com.tradingplatform.listing.dto;

import com.tradingplatform.listing.enums.Condition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for searching/filtering listings.
 * All fields are optional - only provided filters are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingSearchRequest {

    /**
     * Full-text search query for title and description.
     */
    private String query;

    /**
     * Category filter - includes all descendant categories.
     */
    private Long categoryId;

    /**
     * Minimum price filter (inclusive).
     */
    private BigDecimal minPrice;

    /**
     * Maximum price filter (inclusive).
     */
    private BigDecimal maxPrice;

    /**
     * Condition filter - can filter by multiple conditions.
     */
    private List<Condition> conditions;

    /**
     * City filter (case-insensitive).
     */
    private String city;

    /**
     * Region filter (case-insensitive).
     */
    private String region;

    /**
     * Latitude for distance-based search.
     */
    private Double latitude;

    /**
     * Longitude for distance-based search.
     */
    private Double longitude;

    /**
     * Search radius in kilometers.
     */
    private Double radiusKm;
}