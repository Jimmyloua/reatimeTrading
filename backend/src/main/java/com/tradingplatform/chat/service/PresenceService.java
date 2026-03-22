package com.tradingplatform.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final long PRESENCE_TIMEOUT_SECONDS = 60;

    // In production, use Redis for distributed presence tracking
    private final Map<Long, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionLastSeen = new ConcurrentHashMap<>();

    /**
     * Called when a user connects via WebSocket.
     *
     * @param userId the user ID
     */
    public boolean userConnected(Long userId, String sessionId) {
        LocalDateTime now = LocalDateTime.now();
        boolean wasOnline = isUserOnline(userId);

        if (sessionId != null && !sessionId.isBlank()) {
            sessionUsers.put(sessionId, userId);
            userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
            sessionLastSeen.put(sessionId, now);
        }

        onlineUsers.put(userId, now);
        log.debug("User {} connected", userId);
        return !wasOnline;
    }

    /**
     * Called when a user disconnects from WebSocket.
     *
     * @param userId the user ID
     */
    public boolean userDisconnected(Long userId, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            removeSession(sessionId);
        } else {
            removeAllSessions(userId);
        }

        if (hasActiveSession(userId)) {
            onlineUsers.put(userId, latestSessionActivity(userId));
            log.debug("User {} disconnected from one session but remains online", userId);
            return false;
        }

        onlineUsers.remove(userId);
        log.debug("User {} disconnected", userId);
        return true;
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
        return lastSeen.isAfter(LocalDateTime.now().minusSeconds(PRESENCE_TIMEOUT_SECONDS));
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
    public void heartbeat(Long userId, String sessionId) {
        LocalDateTime now = LocalDateTime.now();

        if (sessionId != null && !sessionId.isBlank()) {
            sessionUsers.put(sessionId, userId);
            userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
            sessionLastSeen.put(sessionId, now);
        }

        if (onlineUsers.containsKey(userId) || hasActiveSession(userId)) {
            onlineUsers.put(userId, now);
        }
    }

    /**
     * Expires stale sessions and returns users that became offline.
     *
     * @return the users that transitioned to offline
     */
    public List<Long> expireStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PRESENCE_TIMEOUT_SECONDS);
        List<String> expiredSessionIds = new ArrayList<>();

        for (Map.Entry<String, LocalDateTime> entry : sessionLastSeen.entrySet()) {
            if (entry.getValue().isBefore(cutoff)) {
                expiredSessionIds.add(entry.getKey());
            }
        }

        List<Long> usersBecameOffline = new ArrayList<>();
        for (String sessionId : expiredSessionIds) {
            Long userId = sessionUsers.get(sessionId);
            removeSession(sessionId);

            if (userId == null) {
                continue;
            }

            if (hasActiveSession(userId)) {
                onlineUsers.put(userId, latestSessionActivity(userId));
                continue;
            }

            if (onlineUsers.remove(userId) != null) {
                usersBecameOffline.add(userId);
                log.debug("User {} marked offline after heartbeat timeout", userId);
            }
        }

        return usersBecameOffline;
    }

    private boolean hasActiveSession(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PRESENCE_TIMEOUT_SECONDS);
        for (String sessionId : sessions) {
            LocalDateTime lastSeen = sessionLastSeen.get(sessionId);
            if (lastSeen != null && lastSeen.isAfter(cutoff)) {
                return true;
            }
        }

        return false;
    }

    private LocalDateTime latestSessionActivity(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return LocalDateTime.now();
        }

        LocalDateTime latest = null;
        for (String sessionId : sessions) {
            LocalDateTime lastSeen = sessionLastSeen.get(sessionId);
            if (lastSeen != null && (latest == null || lastSeen.isAfter(latest))) {
                latest = lastSeen;
            }
        }

        return latest == null ? LocalDateTime.now() : latest;
    }

    private void removeSession(String sessionId) {
        Long userId = sessionUsers.remove(sessionId);
        sessionLastSeen.remove(sessionId);

        if (userId == null) {
            return;
        }

        Set<String> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
    }

    private void removeAllSessions(Long userId) {
        Set<String> sessions = userSessions.remove(userId);
        if (sessions == null) {
            return;
        }

        for (String sessionId : sessions) {
            sessionUsers.remove(sessionId);
            sessionLastSeen.remove(sessionId);
        }
    }
}
