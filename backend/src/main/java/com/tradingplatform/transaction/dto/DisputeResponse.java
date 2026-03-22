package com.tradingplatform.transaction.dto;

import com.tradingplatform.transaction.entity.DisputeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for dispute operations.
 */
@Data
@Builder
public class DisputeResponse {
    private Long id;
    private Long transactionId;
    private Long openerId;
    private String openerName;
    private String reason;
    private String description;
    private DisputeStatus status;
    private String resolution;
    private Long resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}