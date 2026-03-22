package com.tradingplatform.transaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for transaction actions (accept, decline, cancel, confirm).
 */
@Data
public class TransactionActionRequest {
    @NotNull
    private String idempotencyKey;

    private String cancellationReason;  // Required for cancel action
}