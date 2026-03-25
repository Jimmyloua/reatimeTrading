package com.tradingplatform.chat.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.chat.dto.PresenceUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatRealtimeEvent event = objectMapper.readValue(message.getBody(), ChatRealtimeEvent.class);
            if (event.getType() == ChatRealtimeEvent.EventType.MESSAGE_DELIVERY
                && event.getRecipientUserId() != null
                && event.getMessage() != null) {
                messagingTemplate.convertAndSendToUser(
                    event.getRecipientUserId().toString(),
                    "/queue/messages",
                    event.getMessage()
                );
                return;
            }

            if (event.getType() == ChatRealtimeEvent.EventType.PRESENCE_UPDATE && event.getPresenceUpdate() != null) {
                PresenceUpdateResponse presenceUpdate = event.getPresenceUpdate();
                messagingTemplate.convertAndSend("/topic/presence." + presenceUpdate.getUserId(), presenceUpdate);
                return;
            }

            log.warn(
                "Ignoring unsupported chat realtime event on channel {}",
                new String(message.getChannel(), StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            log.error("Failed to deserialize chat realtime event", exception);
        }
    }
}
