package com.tradingplatform.transaction.dto;

import com.tradingplatform.transaction.entity.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for transaction summary (used in lists).
 */
@Data
@Builder
public class TransactionResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingImageUrl;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userRole;  // "BUYER" or "SELLER" for current user
}