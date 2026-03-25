package com.tradingplatform.chat.service;

import com.tradingplatform.TradingPlatformApplication;
import com.tradingplatform.chat.dto.PresenceUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TradingPlatformApplication.class)
@ActiveProfiles("test")
@DisplayName("PresenceService Redis contract tests")
class PresenceServiceRedisTest {

    private static final String REDIS_EXECUTABLE =
        System.getProperty("redis.server.executable",
            "D:/Redis/redis8_06/Redis-8.0.6-Windows-x64-msys2-with-Service/redis-server.exe");
    private static final int REDIS_PORT = findFreePort();
    private static Process redisProcess = startRedisProcess();

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
    }

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearRedis() {
        Set<String> keys = redisTemplate.keys("chat:presence:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @AfterAll
    static void stopRedisProcess() {
        if (redisProcess != null && redisProcess.isAlive()) {
            redisProcess.destroy();
        }
    }

    @Test
    @DisplayName("service defines Redis key prefixes, a 60 second timeout, and presence payload updatedAt")
    void serviceDefinesRedisKeyPrefixesTimeoutAndUpdatedAtPayload() throws NoSuchFieldException {
        assertEquals("chat:presence:session:", ReflectionTestUtils.getField(PresenceService.class, "SESSION_KEY_PREFIX"));
        assertEquals("chat:presence:user:", ReflectionTestUtils.getField(PresenceService.class, "USER_KEY_PREFIX"));
        assertEquals(60L, ReflectionTestUtils.getField(PresenceService.class, "PRESENCE_TIMEOUT_SECONDS"));

        Field updatedAtField = PresenceUpdateResponse.class.getDeclaredField("updatedAt");
        assertEquals(LocalDateTime.class, updatedAtField.getType());
    }

    @Test
    @DisplayName("userConnected stores distributed session state in Redis and keeps one seller online across multiple sessions")
    void userConnectedStoresDistributedSessionStateInRedis() {
        assertTrue(presenceService.userConnected(7L, "sess-a"));
        assertFalse(presenceService.userConnected(7L, "sess-b"));

        assertTrue(redisTemplate.opsForValue().get("chat:presence:session:sess-a").startsWith("7|"));
        assertTrue(redisTemplate.opsForValue().get("chat:presence:session:sess-b").startsWith("7|"));
        assertEquals(Set.of("sess-a", "sess-b"), redisTemplate.opsForSet().members("chat:presence:user:7:sessions"));
        assertTrue(presenceService.isUserOnline(7L));
    }

    @Test
    @DisplayName("heartbeat refreshes Redis session TTL within the 60 second timeout")
    void heartbeatRefreshesRedisSessionTtlWithinSixtySeconds() {
        presenceService.userConnected(7L, "sess-a");
        redisTemplate.expire("chat:presence:session:sess-a", Duration.ofSeconds(1));

        presenceService.heartbeat(7L, "sess-a");

        Long ttl = redisTemplate.getExpire("chat:presence:session:sess-a");
        assertNotNull(ttl);
        assertTrue(ttl > 1L, "heartbeat should extend the Redis session TTL back near the 60 second timeout");
    }

    @Test
    @DisplayName("userDisconnected stays false while another Redis-backed session is still active")
    void userDisconnectedStaysFalseWhileAnotherRedisBackedSessionIsStillActive() {
        presenceService.userConnected(7L, "sess-a");
        presenceService.userConnected(7L, "sess-b");

        boolean becameOffline = presenceService.userDisconnected(7L, "sess-a");

        assertFalse(becameOffline);
        assertTrue(presenceService.isUserOnline(7L));
        assertEquals(Set.of("sess-b"), redisTemplate.opsForSet().members("chat:presence:user:7:sessions"));
    }

    @Test
    @DisplayName("expireStaleSessions returns the user id after the 60 second inactivity window expires")
    void expireStaleSessionsReturnsUserAfterInactivityWindowExpires() {
        presenceService.userConnected(7L, "sess-a");
        redisTemplate.opsForValue().set(
            "chat:presence:session:sess-a",
            "7|" + LocalDateTime.now().minusSeconds(61)
        );

        List<Long> expiredUsers = presenceService.expireStaleSessions();

        assertEquals(List.of(7L), expiredUsers);
        assertFalse(presenceService.isUserOnline(7L));
    }

    @Test
    @DisplayName("expired Redis-backed presence should retain last seen text instead of collapsing to Offline")
    void expiredPresenceRetainsLastSeenTextAfterTimeout() {
        presenceService.userConnected(7L, "sess-a");
        redisTemplate.opsForValue().set(
            "chat:presence:session:sess-a",
            "7|" + LocalDateTime.now().minusSeconds(61)
        );

        presenceService.expireStaleSessions();

        assertTrue(
            presenceService.getLastSeenText(7L).startsWith("Last seen"),
            "Redis-backed expiry should preserve the seller's last seen text after timeout"
        );
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to allocate a Redis test port", exception);
        }
    }

    private static Process startRedisProcess() {
        try {
            Process process = new ProcessBuilder(
                REDIS_EXECUTABLE,
                "--port", String.valueOf(REDIS_PORT),
                "--save", "",
                "--appendonly", "no"
            ).start();
            waitForRedis();
            return process;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start local redis-server for tests", exception);
        }
    }

    private static void waitForRedis() {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (System.currentTimeMillis() < deadline) {
            try (Socket ignored = new Socket("127.0.0.1", REDIS_PORT)) {
                return;
            } catch (IOException ignored) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Redis test server", exception);
                }
            }
        }
        throw new IllegalStateException("Timed out waiting for Redis test server to accept connections");
    }
}
