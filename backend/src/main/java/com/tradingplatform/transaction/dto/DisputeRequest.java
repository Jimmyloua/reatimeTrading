package com.tradingplatform.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for opening a dispute.
 */
@Data
public class DisputeRequest {
    @NotBlank(message = "Reason is required")
    @Size(max = 100, message = "Reason must be at most 100 characters")
    private String reason;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;
}