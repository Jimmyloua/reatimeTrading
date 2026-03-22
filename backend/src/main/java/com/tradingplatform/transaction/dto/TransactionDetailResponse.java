package com.tradingplatform.transaction.dto;

import com.tradingplatform.transaction.entity.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for detailed transaction view.
 */
@Data
@Builder
public class TransactionDetailResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingDescription;
    private String listingImageUrl;
    private BigDecimal amount;

    private Long buyerId;
    private String buyerName;
    private String buyerAvatarUrl;

    private Long sellerId;
    private String sellerName;
    private String sellerAvatarUrl;

    private TransactionStatus status;
    private String userRole;

    // Lifecycle timestamps for timeline
    private LocalDateTime createdAt;
    private LocalDateTime fundedAt;
    private LocalDateTime reservedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime settledAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime expiredAt;

    private String cancellationReason;

    // Actions available to current user
    private boolean canCancel;
    private boolean canConfirmPayment;
    private boolean canConfirmFunds;
    private boolean canMarkDelivered;
    private boolean canConfirmReceipt;
    private boolean canRate;
    private boolean canDispute;
}