package com.tradingplatform.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PresenceService Redis contract tests")
class PresenceServiceRedisTest {

    @Test
    @DisplayName("userConnected keeps one seller online across multiple sessions")
    void userConnectedKeepsSellerOnlineAcrossMultipleSessions() {
        PresenceService presenceService = new PresenceService();

        assertTrue(presenceService.userConnected(7L, "sess-a"));
        assertFalse(presenceService.userConnected(7L, "sess-b"));
        assertTrue(presenceService.isUserOnline(7L));
    }

    @Test
    @DisplayName("heartbeat refreshes session activity within the 60 second timeout")
    @SuppressWarnings("unchecked")
    void heartbeatRefreshesSessionActivityWithinSixtySeconds() {
        PresenceService presenceService = new PresenceService();
        presenceService.userConnected(7L, "sess-a");

        Map<String, LocalDateTime> sessionLastSeen =
            (Map<String, LocalDateTime>) ReflectionTestUtils.getField(presenceService, "sessionLastSeen");
        assertNotNull(sessionLastSeen);
        sessionLastSeen.put("sess-a", LocalDateTime.now().minusSeconds(59));

        presenceService.heartbeat(7L, "sess-a");

        List<Long> expired = presenceService.expireStaleSessions();

        assertEquals(60L, ReflectionTestUtils.getField(presenceService, "PRESENCE_TIMEOUT_SECONDS"));
        assertTrue(expired.isEmpty());
        assertTrue(presenceService.isUserOnline(7L));
    }

    @Test
    @DisplayName("userDisconnected stays false while another session is still active")
    void userDisconnectedStaysFalseWhileAnotherSessionIsStillActive() {
        PresenceService presenceService = new PresenceService();
        presenceService.userConnected(7L, "sess-a");
        presenceService.userConnected(7L, "sess-b");

        boolean becameOffline = presenceService.userDisconnected(7L, "sess-a");

        assertFalse(becameOffline);
        assertTrue(presenceService.isUserOnline(7L));
    }

    @Test
    @DisplayName("expireStaleSessions returns the user id after the 60 second inactivity window expires")
    @SuppressWarnings("unchecked")
    void expireStaleSessionsReturnsUserAfterInactivityWindowExpires() {
        PresenceService presenceService = new PresenceService();
        presenceService.userConnected(7L, "sess-a");

        Map<String, LocalDateTime> sessionLastSeen =
            (Map<String, LocalDateTime>) ReflectionTestUtils.getField(presenceService, "sessionLastSeen");
        assertNotNull(sessionLastSeen);
        sessionLastSeen.put("sess-a", LocalDateTime.now().minusSeconds(61));

        List<Long> expiredUsers = presenceService.expireStaleSessions();

        assertEquals(List.of(7L), expiredUsers);
        assertFalse(presenceService.isUserOnline(7L));
    }

    @Test
    @DisplayName("expired Redis-backed presence should retain last seen text instead of collapsing to Offline")
    @SuppressWarnings("unchecked")
    void expiredPresenceRetainsLastSeenTextAfterTimeout() {
        PresenceService presenceService = new PresenceService();
        presenceService.userConnected(7L, "sess-a");

        Map<Long, LocalDateTime> onlineUsers =
            (Map<Long, LocalDateTime>) ReflectionTestUtils.getField(presenceService, "onlineUsers");
        Map<String, LocalDateTime> sessionLastSeen =
            (Map<String, LocalDateTime>) ReflectionTestUtils.getField(presenceService, "sessionLastSeen");
        assertNotNull(onlineUsers);
        assertNotNull(sessionLastSeen);

        LocalDateTime expiredAt = LocalDateTime.now().minusSeconds(61);
        onlineUsers.put(7L, expiredAt);
        sessionLastSeen.put("sess-a", expiredAt);

        presenceService.expireStaleSessions();

        assertTrue(
            presenceService.getLastSeenText(7L).startsWith("Last seen"),
            "Redis-backed expiry should preserve the seller's last seen text after timeout"
        );
    }
}
