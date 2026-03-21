package com.tradingplatform.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for WebSocket message sending.
 * Used with @MessageMapping for real-time chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageRequest {
    @NotNull
    @Positive
    private Long conversationId;

    @Size(max = 5000)
    private String content;

    private String imageUrl;

    public boolean hasContent() {
        return (content != null && !content.isBlank()) || imageUrl != null;
    }
}