package com.tradingplatform.chat.service;

import com.tradingplatform.chat.dto.PresenceUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cleans up stale WebSocket sessions so presence can recover from silent disconnects.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceSessionMaintenance {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 15000)
    public void expireStaleSessions() {
        for (Long userId : presenceService.expireStaleSessions()) {
            messagingTemplate.convertAndSend(
                "/topic/presence." + userId,
                PresenceUpdateResponse.builder()
                    .userId(userId)
                    .online(false)
                    .lastSeenText(presenceService.getLastSeenText(userId))
                    .build()
            );
            log.debug("Broadcast offline presence after timeout for user {}", userId);
        }
    }
}
