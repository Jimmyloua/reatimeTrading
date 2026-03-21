package com.tradingplatform.chat.dto;

import lombok.*;

/**
 * Response DTO for typing indicator broadcast.
 * Implements CHAT-05: Activity indicators.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingResponse {
    private Long userId;
    private String username;
    private boolean typing;
}