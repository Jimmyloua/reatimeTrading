package com.tradingplatform.transaction.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserRatingSummary {
    private Long userId;
    private BigDecimal averageRating;
    private Integer totalRatings;
    private Boolean hasRatings;
}