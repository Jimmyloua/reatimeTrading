package com.tradingplatform.transaction.dto;

import com.tradingplatform.transaction.entity.DisputeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for resolving a dispute (admin action).
 */
@Data
public class DisputeResolutionRequest {
    @NotNull(message = "Resolution status is required")
    private DisputeStatus status;  // RESOLVED_BUYER, RESOLVED_SELLER, PARTIALLY_RESOLVED

    private String resolution;  // Admin's explanation

    private Long adminId;  // For audit
}