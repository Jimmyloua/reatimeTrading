package com.tradingplatform.chat.dto;

import lombok.*;

/**
 * Request DTO for typing indicator.
 * Implements CHAT-05: Activity indicators.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingRequest {
    private Long conversationId;
    private boolean typing;
}