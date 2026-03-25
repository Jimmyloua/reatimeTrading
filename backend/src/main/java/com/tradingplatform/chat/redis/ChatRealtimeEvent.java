package com.tradingplatform.chat.redis;

import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.chat.dto.PresenceUpdateResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRealtimeEvent {

    private EventType type;
    private Long recipientUserId;
    private Long conversationId;
    private MessageResponse message;
    private PresenceUpdateResponse presenceUpdate;

    public enum EventType {
        MESSAGE_DELIVERY,
        PRESENCE_UPDATE
    }
}
