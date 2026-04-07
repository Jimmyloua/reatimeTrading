package com.tradingplatform.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.auth.dto.RegisterRequest;
import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationPreference;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.repository.NotificationPreferenceRepository;
import com.tradingplatform.notification.repository.NotificationRepository;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController.
 * Covers NOTF-03 and NOTF-04.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {
    private static final String REDIS_EXECUTABLE =
            System.getProperty("redis.server.executable",
                    "D:/Redis/redis8_06/Redis-8.0.6-Windows-x64-msys2-with-Service/redis-server.exe");
    private static final int REDIS_PORT = findFreePort();
    private static final Process REDIS_PROCESS = startRedisProcess();

    /**
     * Phase 5 Wave 0 note:
     * 05-00 reserves controller coverage slots that 05-01 will harden into the
     * final NOTF-06/NOTF-07 contract if the endpoint shape changes during implementation.
     */

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private String accessToken;
    private Long testUserId;

    @BeforeEach
    void setUp() throws Exception {
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        userRepository.deleteAll();

        // Register a test user
        RegisterRequest registerRequest = new RegisterRequest("notif@example.com", "password123");
        MvcResult result = performResolved(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("accessToken").asText();
        testUserId = userRepository.findByEmail("notif@example.com").orElseThrow().getId();
    }

    @Test
    @DisplayName("GET /api/notifications returns notifications for authenticated user (NOTF-03)")
    void getNotifications_authenticated_returnsNotifications() throws Exception {
        // Arrange
        Notification older = createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Test Notification 1");
        older.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 0));
        notificationRepository.save(older);

        Notification newer = createTestNotification(testUserId, NotificationType.ITEM_SOLD, "Test Notification 2");
        newer.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 5));
        notificationRepository.save(newer);

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Test Notification 2")) // Most recent first
                .andExpect(jsonPath("$.content[1].title").value("Test Notification 1"));
    }

    @Test
    @DisplayName("GET /api/notifications returns empty page when no notifications")
    void getNotifications_noNotifications_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/notifications without auth returns 401")
    void getNotifications_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notifications/unread returns only unread notifications")
    void getUnreadNotifications_returnsUnreadOnly() throws Exception {
        // Arrange
        Notification unread = createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Unread");
        Notification read = createTestNotification(testUserId, NotificationType.ITEM_SOLD, "Read");
        read.setRead(true);
        notificationRepository.save(read);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Unread"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @DisplayName("GET /api/notifications/count returns unread count")
    void getUnreadCount_returnsCount() throws Exception {
        // Arrange
        createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Unread1");
        createTestNotification(testUserId, NotificationType.ITEM_SOLD, "Unread2");
        Notification read = createTestNotification(testUserId, NotificationType.SYSTEM_ANNOUNCEMENT, "Read");
        read.setRead(true);
        notificationRepository.save(read);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    @Test
    @DisplayName("GET /api/notifications/preferences returns default enabled preferences")
    void getPreferences_returnsDefaults() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newMessageEnabled").value(true))
                .andExpect(jsonPath("$.itemSoldEnabled").value(true))
                .andExpect(jsonPath("$.transactionUpdateEnabled").value(true));
    }

    @Test
    @DisplayName("PATCH /api/notifications/preferences persists partial preference updates")
    void patchPreferences_persistsPartialUpdate() throws Exception {
        mockMvc.perform(patch("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newMessageEnabled": false,
                                  "transactionUpdateEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newMessageEnabled").value(false))
                .andExpect(jsonPath("$.itemSoldEnabled").value(true))
                .andExpect(jsonPath("$.transactionUpdateEnabled").value(false));

        NotificationPreference saved = notificationPreferenceRepository.findByUserId(testUserId).orElseThrow();
        assertThat(saved.getNewMessageEnabled()).isFalse();
        assertThat(saved.getItemSoldEnabled()).isTrue();
        assertThat(saved.getTransactionUpdateEnabled()).isFalse();
    }

    @Test
    @DisplayName("PATCH /api/notifications/preferences merges updates and GET returns persisted state")
    void patchPreferences_thenGetPreferences_returnsPersistedState() throws Exception {
        mockMvc.perform(patch("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemSoldEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newMessageEnabled").value(true))
                .andExpect(jsonPath("$.itemSoldEnabled").value(false))
                .andExpect(jsonPath("$.transactionUpdateEnabled").value(true));

        mockMvc.perform(get("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newMessageEnabled").value(true))
                .andExpect(jsonPath("$.itemSoldEnabled").value(false))
                .andExpect(jsonPath("$.transactionUpdateEnabled").value(true));
    }

    @Test
    @DisplayName("Notification preference endpoints require authentication")
    void notificationPreferenceEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newMessageEnabled": false
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read marks notification as read (NOTF-04)")
    void markAsRead_existingNotification_marksAsRead() throws Exception {
        // Arrange
        Notification notif = createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Test");
        assertThat(notif.getRead()).isFalse();

        // Act & Assert
        mockMvc.perform(patch("/api/notifications/{id}/read", notif.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notif.getId()))
                .andExpect(jsonPath("$.content").value("Content for Test"))
                .andExpect(jsonPath("$.read").value(true));

        // Verify notification is now read
        Notification updated = notificationRepository.findById(notif.getId()).orElseThrow();
        assertThat(updated.getRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read returns 404 for non-existent notification")
    void markAsRead_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/notifications/{id}/read", 99999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all marks all notifications as read (NOTF-04)")
    void markAllAsRead_marksAllAsRead() throws Exception {
        // Arrange
        createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Unread1");
        createTestNotification(testUserId, NotificationType.ITEM_SOLD, "Unread2");
        createTestNotification(testUserId, NotificationType.TRANSACTION_UPDATE, "Unread3");

        // Act & Assert
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Verify all notifications are now read
        Long unreadCount = notificationRepository.countByUserIdAndReadFalse(testUserId);
        assertThat(unreadCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /api/notifications filters by tab and types query params for notification management")
    void getNotifications_withTabAndTypes_filtersVisibleRows() throws Exception {
        createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Unread message");
        createTestNotification(testUserId, NotificationType.ITEM_SOLD, "Unread sold");
        Notification readTransaction = createTestNotification(
                testUserId,
                NotificationType.TRANSACTION_UPDATE,
                "Read transaction");
        readTransaction.setRead(true);
        notificationRepository.save(readTransaction);

        mockMvc.perform(get("/api/notifications")
                        .param("tab", "unread")
                        .param("types", "NEW_MESSAGE,ITEM_SOLD")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].type", containsInAnyOrder("NEW_MESSAGE", "ITEM_SOLD")))
                .andExpect(jsonPath("$.content[*].read", everyItem(is(false))));
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-visible marks only the filtered unread notifications as read")
    void markVisibleAsRead_marksOnlyFilteredNotifications() throws Exception {
        Notification visibleMessage = createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Visible message");
        Notification hiddenSold = createTestNotification(testUserId, NotificationType.ITEM_SOLD, "Hidden sold");

        mockMvc.perform(patch("/api/notifications/read-visible")
                        .param("tab", "unread")
                        .param("types", "NEW_MESSAGE")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        Notification updatedMessage = notificationRepository.findById(visibleMessage.getId()).orElseThrow();
        Notification unchangedSold = notificationRepository.findById(hiddenSold.getId()).orElseThrow();

        assertThat(updatedMessage.getRead()).isTrue();
        assertThat(unchangedSold.getRead()).isFalse();
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-visible with tab all and no types marks the whole visible set read")
    void markVisibleAsRead_withAllTabAndNoTypes_marksVisibleSet() throws Exception {
        createTestNotification(testUserId, NotificationType.NEW_MESSAGE, "Unread1");
        createTestNotification(testUserId, NotificationType.ITEM_SOLD, "Unread2");
        createTestNotification(testUserId, NotificationType.TRANSACTION_UPDATE, "Unread3");

        mockMvc.perform(patch("/api/notifications/read-visible")
                        .param("tab", "all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(notificationRepository.countByUserIdAndReadFalse(testUserId)).isEqualTo(0L);
    }

    private Notification createTestNotification(Long userId, NotificationType type, String title) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content("Content for " + title)
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }

    private ResultActions performResolved(RequestBuilder requestBuilder) throws Exception {
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        MvcResult mvcResult = resultActions.andReturn();
        if (!mvcResult.getRequest().isAsyncStarted()) {
            return resultActions;
        }
        if (mvcResult.getRequest().getUserPrincipal() instanceof Authentication authenticationToken) {
            try {
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                return mockMvc.perform(asyncDispatch(mvcResult));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
        return mockMvc.perform(asyncDispatch(mvcResult));
    }

    @AfterAll
    static void stopRedisProcess() {
        if (REDIS_PROCESS != null && REDIS_PROCESS.isAlive()) {
            REDIS_PROCESS.destroy();
        }
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
                    "--port", String.valueOf(REDIS_PORT)
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
