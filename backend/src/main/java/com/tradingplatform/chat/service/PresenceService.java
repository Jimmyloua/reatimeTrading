package com.tradingplatform.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Service for tracking user online presence.
 * Implements D-10: Online/offline presence shown per user.
 * Implements D-11: Presence status visible in chat header and conversation list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    static final String SESSION_KEY_PREFIX = "chat:presence:session:";
    static final String USER_KEY_PREFIX = "chat:presence:user:";
    static final long PRESENCE_TIMEOUT_SECONDS = 60;

    private static final String SESSION_SET_SUFFIX = ":sessions";
    private static final String LAST_SEEN_SUFFIX = ":last-seen";
    private static final String VALUE_DELIMITER = "|";

    private final StringRedisTemplate redisTemplate;

    public boolean userConnected(Long userId, String sessionId) {
        boolean wasOnline = isUserOnline(userId);
        LocalDateTime now = LocalDateTime.now();

        if (hasSessionId(sessionId)) {
            storeSession(userId, sessionId, now);
        }
        storeLastSeen(userId, now);
        log.debug("User {} connected", userId);
        return !wasOnline;
    }

    public boolean userDisconnected(Long userId, String sessionId) {
        LocalDateTime now = LocalDateTime.now();
        if (hasSessionId(sessionId)) {
            removeSession(userId, sessionId);
        } else {
            removeAllSessions(userId);
        }
        storeLastSeen(userId, now);

        if (hasActiveSession(userId)) {
            log.debug("User {} disconnected from one session but remains online", userId);
            return false;
        }

        redisTemplate.delete(userSessionSetKey(userId));
        log.debug("User {} disconnected", userId);
        return true;
    }

    public boolean isUserOnline(Long userId) {
        return hasActiveSession(userId);
    }

    public String getLastSeenText(Long userId) {
        if (isUserOnline(userId)) {
            return "Online";
        }

        LocalDateTime lastSeen = getLastActivityAt(userId);
        if (lastSeen == null) {
            return "Offline";
        }

        long seconds = Duration.between(lastSeen, LocalDateTime.now()).getSeconds();
        if (seconds < 60) return "Last seen just now";
        if (seconds < 3600) return "Last seen " + (seconds / 60) + "m ago";
        if (seconds < 86400) return "Last seen " + (seconds / 3600) + "h ago";
        return "Last seen " + (seconds / 86400) + "d ago";
    }

    public LocalDateTime getLastActivityAt(Long userId) {
        List<LocalDateTime> candidates = new ArrayList<>();
        LocalDateTime storedLastSeen = readLastSeen(userId);
        if (storedLastSeen != null) {
            candidates.add(storedLastSeen);
        }

        Set<String> sessions = redisTemplate.opsForSet().members(userSessionSetKey(userId));
        if (sessions != null) {
            for (String sessionId : sessions) {
                SessionPresence sessionPresence = readSessionPresence(sessionId);
                if (sessionPresence != null) {
                    candidates.add(sessionPresence.lastSeen());
                }
            }
        }

        return candidates.stream().max(Comparator.naturalOrder()).orElse(null);
    }

    public void heartbeat(Long userId, String sessionId) {
        if (!hasSessionId(sessionId)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        storeSession(userId, sessionId, now);
        storeLastSeen(userId, now);
    }

    public List<Long> expireStaleSessions() {
        List<Long> usersBecameOffline = new ArrayList<>();
        Set<String> userSessionKeys = redisTemplate.keys(USER_KEY_PREFIX + "*"+ SESSION_SET_SUFFIX);
        if (userSessionKeys == null || userSessionKeys.isEmpty()) {
            return usersBecameOffline;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PRESENCE_TIMEOUT_SECONDS);
        for (String sessionSetKey : userSessionKeys) {
            Long userId = extractUserId(sessionSetKey);
            if (userId == null) {
                continue;
            }

            Set<String> sessions = redisTemplate.opsForSet().members(sessionSetKey);
            if (sessions == null || sessions.isEmpty()) {
                redisTemplate.delete(sessionSetKey);
                continue;
            }

            boolean hadSessions = !sessions.isEmpty();
            LocalDateTime latestExpiredSeen = null;
            for (String sessionId : sessions) {
                SessionPresence sessionPresence = readSessionPresence(sessionId);
                if (sessionPresence == null) {
                    redisTemplate.opsForSet().remove(sessionSetKey, sessionId);
                    continue;
                }

                if (!sessionPresence.lastSeen().isAfter(cutoff)) {
                    redisTemplate.delete(sessionKey(sessionId));
                    redisTemplate.opsForSet().remove(sessionSetKey, sessionId);
                    if (latestExpiredSeen == null || sessionPresence.lastSeen().isAfter(latestExpiredSeen)) {
                        latestExpiredSeen = sessionPresence.lastSeen();
                    }
                }
            }

            Set<String> remainingSessions = redisTemplate.opsForSet().members(sessionSetKey);
            if (remainingSessions == null || remainingSessions.isEmpty()) {
                redisTemplate.delete(sessionSetKey);
                if (latestExpiredSeen != null) {
                    storeLastSeen(userId, latestExpiredSeen);
                }
                if (hadSessions) {
                    usersBecameOffline.add(userId);
                    log.debug("User {} marked offline after heartbeat timeout", userId);
                }
            }
        }

        return usersBecameOffline;
    }

    private boolean hasActiveSession(Long userId) {
        Set<String> sessions = redisTemplate.opsForSet().members(userSessionSetKey(userId));
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PRESENCE_TIMEOUT_SECONDS);
        boolean hasActive = false;
        for (String sessionId : sessions) {
            SessionPresence sessionPresence = readSessionPresence(sessionId);
            if (sessionPresence == null) {
                redisTemplate.opsForSet().remove(userSessionSetKey(userId), sessionId);
                continue;
            }

            if (sessionPresence.lastSeen().isAfter(cutoff)) {
                hasActive = true;
            } else {
                redisTemplate.delete(sessionKey(sessionId));
                redisTemplate.opsForSet().remove(userSessionSetKey(userId), sessionId);
                storeLastSeen(userId, sessionPresence.lastSeen());
            }
        }

        return hasActive;
    }

    private void storeSession(Long userId, String sessionId, LocalDateTime lastSeen) {
        redisTemplate.opsForSet().add(userSessionSetKey(userId), sessionId);
        redisTemplate.opsForValue().set(
            sessionKey(sessionId),
            userId + VALUE_DELIMITER + lastSeen,
            Duration.ofSeconds(PRESENCE_TIMEOUT_SECONDS)
        );
    }

    private void storeLastSeen(Long userId, LocalDateTime lastSeen) {
        redisTemplate.opsForValue().set(lastSeenKey(userId), lastSeen.toString());
    }

    private LocalDateTime readLastSeen(Long userId) {
        String value = redisTemplate.opsForValue().get(lastSeenKey(userId));
        return value == null ? null : LocalDateTime.parse(value);
    }

    private SessionPresence readSessionPresence(String sessionId) {
        String value = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            return null;
        }
        return new SessionPresence(Long.parseLong(parts[0]), LocalDateTime.parse(parts[1]));
    }

    private void removeSession(Long userId, String sessionId) {
        redisTemplate.delete(sessionKey(sessionId));
        redisTemplate.opsForSet().remove(userSessionSetKey(userId), sessionId);
    }

    private void removeAllSessions(Long userId) {
        Set<String> sessions = redisTemplate.opsForSet().members(userSessionSetKey(userId));
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        for (String sessionId : sessions) {
            redisTemplate.delete(sessionKey(sessionId));
        }
        redisTemplate.delete(userSessionSetKey(userId));
    }

    private Long extractUserId(String sessionSetKey) {
        String prefix = USER_KEY_PREFIX;
        String suffix = SESSION_SET_SUFFIX;
        if (!sessionSetKey.startsWith(prefix) || !sessionSetKey.endsWith(suffix)) {
            return null;
        }
        return Long.parseLong(sessionSetKey.substring(prefix.length(), sessionSetKey.length() - suffix.length()));
    }

    private boolean hasSessionId(String sessionId) {
        return sessionId != null && !sessionId.isBlank();
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionSetKey(Long userId) {
        return USER_KEY_PREFIX + userId + SESSION_SET_SUFFIX;
    }

    private String lastSeenKey(Long userId) {
        return USER_KEY_PREFIX + userId + LAST_SEEN_SUFFIX;
    }

    private record SessionPresence(Long userId, LocalDateTime lastSeen) {
    }
}
