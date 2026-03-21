package com.tradingplatform.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking user online presence.
 * Implements D-10: Online/offline presence shown per user.
 * Implements D-11: Presence status visible in chat header and conversation list.
 *
 * Note: In production, use Redis for distributed presence tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    // In production, use Redis for distributed presence tracking
    private final Map<Long, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();

    /**
     * Called when a user connects via WebSocket.
     *
     * @param userId the user ID
     */
    public void userConnected(Long userId) {
        onlineUsers.put(userId, LocalDateTime.now());
        log.debug("User {} connected", userId);
    }

    /**
     * Called when a user disconnects from WebSocket.
     *
     * @param userId the user ID
     */
    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);
        log.debug("User {} disconnected", userId);
    }

    /**
     * Checks if a user is currently online.
     * User is considered offline after 60 seconds of inactivity.
     *
     * @param userId the user ID
     * @return true if online, false otherwise
     */
    public boolean isUserOnline(Long userId) {
        LocalDateTime lastSeen = onlineUsers.get(userId);
        if (lastSeen == null) return false;

        // Consider offline if no activity for 60 seconds
        return lastSeen.isAfter(LocalDateTime.now().minusSeconds(60));
    }

    /**
     * Gets a human-readable last seen text.
     *
     * @param userId the user ID
     * @return "Online", "Last seen X ago", or "Offline"
     */
    public String getLastSeenText(Long userId) {
        if (isUserOnline(userId)) {
            return "Online";
        }
        LocalDateTime lastSeen = onlineUsers.get(userId);
        if (lastSeen == null) {
            return "Offline";
        }
        // Format relative time (simplified)
        long seconds = java.time.Duration.between(lastSeen, LocalDateTime.now()).getSeconds();
        if (seconds < 60) return "Last seen just now";
        if (seconds < 3600) return "Last seen " + (seconds / 60) + "m ago";
        if (seconds < 86400) return "Last seen " + (seconds / 3600) + "h ago";
        return "Last seen " + (seconds / 86400) + "d ago";
    }

    /**
     * Updates the user's last activity timestamp.
     * Called on heartbeat to keep presence alive.
     *
     * @param userId the user ID
     */
    public void heartbeat(Long userId) {
        if (onlineUsers.containsKey(userId)) {
            onlineUsers.put(userId, LocalDateTime.now());
        }
    }
}