package com.tradingplatform.transaction.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RatingResponse {
    private Long id;
    private Long transactionId;
    private Long raterId;
    private String raterName;
    private Long ratedUserId;
    private Integer rating;
    private String reviewText;
    private Boolean visible;
    private LocalDateTime createdAt;
}