package com.tradingplatform.chat.dto;

import com.tradingplatform.chat.entity.MessageStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for message acknowledgment.
 * Confirms message was persisted and delivered.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAck {
    private String clientMessageId;
    private Long messageId;
    private Long conversationId;
    private MessageStatus status;
    private LocalDateTime createdAt;
}
