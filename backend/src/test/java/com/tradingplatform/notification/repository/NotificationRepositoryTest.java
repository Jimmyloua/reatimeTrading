package com.tradingplatform.notification.repository;

import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for NotificationRepository.
 * Covers NOTF-03 (view notification history) and NOTF-04 (mark as read).
 */
@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should find notifications by user ID ordered by creation date descending")
    void testFindByUserIdOrderByCreatedAtDesc() {
        // Arrange
        Notification notif1 = createNotification(testUserId, NotificationType.NEW_MESSAGE, "First");
        Notification notif2 = createNotification(testUserId, NotificationType.ITEM_SOLD, "Second");
        Notification notif3 = createNotification(999L, NotificationType.SYSTEM_ANNOUNCEMENT, "Other user");

        notificationRepository.save(notif1);
        notificationRepository.save(notif2);
        notificationRepository.save(notif3);

        // Act
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUserId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Second"); // Most recent first
        assertThat(result.get(1).getTitle()).isEqualTo("First");
    }

    @Test
    @DisplayName("Should count unread notifications for user")
    void testCountByUserIdAndReadFalse() {
        // Arrange
        Notification read = createNotification(testUserId, NotificationType.NEW_MESSAGE, "Read");
        read.setRead(true);
        Notification unread1 = createNotification(testUserId, NotificationType.ITEM_SOLD, "Unread1");
        Notification unread2 = createNotification(testUserId, NotificationType.TRANSACTION_UPDATE, "Unread2");

        notificationRepository.save(read);
        notificationRepository.save(unread1);
        notificationRepository.save(unread2);

        // Act
        Long count = notificationRepository.countByUserIdAndReadFalse(testUserId);

        // Assert
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should mark all notifications as read for user")
    void testMarkAllAsReadByUserId() {
        // Arrange
        Notification unread1 = createNotification(testUserId, NotificationType.NEW_MESSAGE, "Unread1");
        Notification unread2 = createNotification(testUserId, NotificationType.ITEM_SOLD, "Unread2");
        notificationRepository.save(unread1);
        notificationRepository.save(unread2);

        LocalDateTime readAt = LocalDateTime.now();

        // Act
        int updated = notificationRepository.markAllAsReadByUserId(testUserId, readAt);

        // Assert
        assertThat(updated).isEqualTo(2);
        List<Notification> afterUpdate = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUserId);
        assertThat(afterUpdate).allMatch(n -> n.getRead() == true);
    }

    @Test
    @DisplayName("Should mark single notification as read")
    void testMarkAsRead() {
        // Arrange
        Notification notif = createNotification(testUserId, NotificationType.NEW_MESSAGE, "Test");
        notif = notificationRepository.save(notif);
        LocalDateTime readAt = LocalDateTime.now();

        // Act
        int updated = notificationRepository.markAsRead(notif.getId(), testUserId, readAt);

        // Assert
        assertThat(updated).isEqualTo(1);
        Notification updatedNotif = notificationRepository.findById(notif.getId()).orElseThrow();
        assertThat(updatedNotif.getRead()).isTrue();
        assertThat(updatedNotif.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return at most 50 unread notifications per D-13")
    void testFindTop50UnreadByUserId() {
        // Arrange - create 60 unread notifications
        for (int i = 0; i < 60; i++) {
            Notification notif = createNotification(testUserId, NotificationType.NEW_MESSAGE, "Notif" + i);
            notificationRepository.save(notif);
        }

        // Act
        List<Notification> result = notificationRepository.findTop50UnreadByUserId(testUserId);

        // Assert
        assertThat(result).hasSize(50);
        assertThat(result).allMatch(n -> !n.getRead());
    }

    @Test
    @DisplayName("Should delete notifications older than 30 days per D-13")
    void testDeleteOlderThan() {
        // Arrange - create notifications and update their created_at via native query
        Notification old = notificationRepository.save(createNotification(testUserId, NotificationType.NEW_MESSAGE, "Old"));
        Notification recent = notificationRepository.save(createNotification(testUserId, NotificationType.NEW_MESSAGE, "Recent"));

        // Use native query to bypass @CreatedDate and set old dates
        LocalDateTime oldDate = LocalDateTime.now().minusDays(31);
        LocalDateTime recentDate = LocalDateTime.now().minusDays(15);

        entityManager.createNativeQuery("UPDATE notifications SET created_at = :date WHERE id = :id")
                .setParameter("date", oldDate)
                .setParameter("id", old.getId())
                .executeUpdate();

        entityManager.createNativeQuery("UPDATE notifications SET created_at = :date WHERE id = :id")
                .setParameter("date", recentDate)
                .setParameter("id", recent.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        // Act
        int deleted = notificationRepository.deleteOlderThan(cutoff);

        // Assert
        assertThat(deleted).isEqualTo(1);
        List<Notification> remaining = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUserId);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getTitle()).isEqualTo("Recent");
    }

    private Notification createNotification(Long userId, NotificationType type, String title) {
        return Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content("Content for " + title)
                .read(false)
                .build();
    }
}