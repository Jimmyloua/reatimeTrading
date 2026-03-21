package com.tradingplatform.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for sending a message in a conversation.
 * Implements D-07: Image sharing supported in chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "Conversation ID is required")
    @Positive(message = "Conversation ID must be positive")
    private Long conversationId;

    /**
     * Message text content (optional if imageUrl provided).
     */
    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    private String content;

    /**
     * Optional image URL for image sharing.
     * Implements D-07: image sharing supported in chat.
     */
    private String imageUrl;

    /**
     * Checks if the message has any content (text or image).
     *
     * @return true if content or imageUrl is provided
     */
    public boolean hasContent() {
        return (content != null && !content.isBlank()) || imageUrl != null;
    }
}