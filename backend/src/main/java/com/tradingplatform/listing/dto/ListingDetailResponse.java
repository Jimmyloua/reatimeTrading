package com.tradingplatform.listing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for detailed listing view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDetailResponse {

    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    private Condition condition;

    private ListingStatus status;

    private String city;

    private String region;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double latitude;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double longitude;

    private List<ImageInfo> images;

    private Long categoryId;

    private String categoryName;

    private SellerInfo seller;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Nested class for image information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageInfo {
        private Long id;
        private String imageUrl;
        private boolean isPrimary;
        private Integer displayOrder;
    }

    /**
     * Nested class for seller information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;
        private LocalDateTime memberSince;
        private Long listingCount;
    }
}