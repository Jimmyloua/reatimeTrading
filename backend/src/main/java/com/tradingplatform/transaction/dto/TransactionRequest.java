package com.tradingplatform.transaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating a new transaction.
 */
@Data
public class TransactionRequest {
    @NotNull
    private Long listingId;

    private Long conversationId;  // Optional: link to existing conversation

    @NotNull
    private String idempotencyKey;  // For duplicate prevention
}