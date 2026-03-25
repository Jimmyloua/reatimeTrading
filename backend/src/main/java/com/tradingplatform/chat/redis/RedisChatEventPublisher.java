package com.tradingplatform.chat.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.chat.dto.PresenceUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishMessageDelivery(Long recipientUserId, Long conversationId, MessageResponse message) {
        publish(
            RedisChannels.MESSAGE_CHANNEL,
            ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.MESSAGE_DELIVERY)
                .recipientUserId(recipientUserId)
                .conversationId(conversationId)
                .message(message)
                .build()
        );
    }

    public void publishPresenceUpdate(PresenceUpdateResponse presenceUpdate) {
        publish(
            RedisChannels.PRESENCE_CHANNEL,
            ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.PRESENCE_UPDATE)
                .presenceUpdate(presenceUpdate)
                .build()
        );
    }

    private void publish(String channel, ChatRealtimeEvent event) {
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chat realtime event for channel " + channel, exception);
        } catch (RuntimeException exception) {
            log.error("Failed to publish chat realtime event to {}", channel, exception);
            throw exception;
        }
    }
}
