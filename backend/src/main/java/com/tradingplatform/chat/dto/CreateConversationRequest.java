package com.tradingplatform.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Request DTO for creating a new conversation.
 * Implements CHAT-01: User can initiate chat with seller about an item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotNull(message = "Listing ID is required")
    @Positive(message = "Listing ID must be positive")
    private Long listingId;

    /**
     * Optional initial message to send when creating the conversation.
     */
    private String initialMessage;
}