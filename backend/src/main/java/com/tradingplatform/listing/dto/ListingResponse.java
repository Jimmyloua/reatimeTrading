package com.tradingplatform.listing.dto;

import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for listing summary (used in lists).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {

    private Long id;

    private String title;

    private BigDecimal price;

    private Condition condition;

    private ListingStatus status;

    private String city;

    private String region;

    private String primaryImageUrl;

    private Long categoryId;

    private String categoryName;

    private LocalDateTime createdAt;
}