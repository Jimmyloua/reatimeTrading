package com.tradingplatform.chat.kafka;

import com.tradingplatform.chat.entity.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDeliveryEvent {

    private Long messageId;
    private Long conversationId;
    private Long recipientUserId;
    private Long senderId;
    private MessageStatus status;
    private LocalDateTime createdAt;
}
