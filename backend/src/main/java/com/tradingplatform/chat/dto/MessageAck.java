package com.tradingplatform.chat.dto;

import com.tradingplatform.chat.entity.MessageStatus;
import lombok.*;

/**
 * Response DTO for message acknowledgment.
 * Confirms message was persisted and delivered.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAck {
    private Long messageId;
    private MessageStatus status;
    private String tempId;  // Client's temporary ID for correlation
}