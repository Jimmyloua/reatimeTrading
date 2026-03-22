package com.tradingplatform.notification.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.notification.dto.NotificationResponse;
import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for NotificationService.
 * Covers NOTF-01, NOTF-02, NOTF-03, and NOTF-04.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    /**
     * Phase 5 Wave 0 note:
     * 05-00 keeps service validation green while reserving explicit hooks for 05-01
     * to harden preference persistence and canonical reference normalization behavior.
     */

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
    }

    @Test
    @DisplayName("Should create notification for new message (NOTF-01)")
    void testCreateMessageNotification() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // Act
        Notification result = notificationService.createNotification(
                testUserId,
                NotificationType.NEW_MESSAGE,
                "New Message",
                "You have a new message from John",
                100L,
                "conversation"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getType()).isEqualTo(NotificationType.NEW_MESSAGE);
        assertThat(result.getTitle()).isEqualTo("New Message");
        assertThat(result.getRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should create notification for item sold (NOTF-02)")
    void testCreateItemSoldNotification() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // Act
        Notification result = notificationService.createNotification(
                testUserId,
                NotificationType.ITEM_SOLD,
                "Item Sold",
                "Your item 'iPhone 15' has been sold!",
                200L,
                "listing"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.ITEM_SOLD);
        assertThat(result.getReferenceType()).isEqualTo("listing");
    }

    @Test
    @DisplayName("Should normalize reference type to lowercase canonical value")
    void testCreateNotification_normalizesReferenceType() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.createNotification(
                testUserId,
                NotificationType.TRANSACTION_UPDATE,
                "Transaction Update",
                "Transaction status: pending",
                300L,
                "TRANSACTION"
        );

        assertThat(result.getReferenceType()).isEqualTo("transaction");
    }

    @Test
    @DisplayName("Phase 5 scaffold: 05-01 may extend normalization coverage to null and mixed-case inputs")
    void phase5Scaffold_referenceNormalizationSlot() {
        // Placeholder for stricter NOTF-07 edge-case assertions once the final
        // canonicalization rules are locked in by 05-01.
    }

    @Test
    @DisplayName("Should return paginated notifications for user (NOTF-03)")
    void testGetNotifications() {
        // Arrange
        Notification notif1 = Notification.builder()
                .id(1L).userId(testUserId).type(NotificationType.NEW_MESSAGE)
                .title("Title1").content("Content1").read(false).build();
        Notification notif2 = Notification.builder()
                .id(2L).userId(testUserId).type(NotificationType.ITEM_SOLD)
                .title("Title2").content("Content2").read(true).build();

        Page<Notification> page = new PageImpl<>(List.of(notif1, notif2));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(testUserId), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        // Act
        Page<NotificationResponse> result = notificationService.getNotifications(testUserId, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Title1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("Title2");
    }

    @Test
    @DisplayName("Should return unread count for user")
    void testGetUnreadCount() {
        // Arrange
        when(notificationRepository.countByUserIdAndReadFalse(testUserId)).thenReturn(5L);

        // Act
        Long count = notificationService.getUnreadCount(testUserId);

        // Assert
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should return unread notifications for user")
    void testGetUnreadNotifications() {
        // Arrange
        Notification notif1 = Notification.builder()
                .id(1L).userId(testUserId).type(NotificationType.NEW_MESSAGE)
                .title("Title1").content("Content1").read(false).build();
        when(notificationRepository.findTop50UnreadByUserId(testUserId)).thenReturn(List.of(notif1));

        // Act
        List<NotificationResponse> result = notificationService.getUnreadNotifications(testUserId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("Should mark notification as read (NOTF-04)")
    void testMarkAsRead() {
        // Arrange
        when(notificationRepository.markAsRead(eq(1L), eq(testUserId), any())).thenReturn(1);

        // Act
        notificationService.markAsRead(1L, testUserId);

        // Assert
        verify(notificationRepository).markAsRead(eq(1L), eq(testUserId), any());
    }

    @Test
    @DisplayName("Should throw exception when marking non-existent notification as read")
    void testMarkAsRead_NotFound() {
        // Arrange
        when(notificationRepository.markAsRead(eq(999L), eq(testUserId), any())).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> notificationService.markAsRead(999L, testUserId))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("Should mark all notifications as read for user (NOTF-04)")
    void testMarkAllAsRead() {
        // Arrange
        when(notificationRepository.markAllAsReadByUserId(eq(testUserId), any())).thenReturn(3);

        // Act
        notificationService.markAllAsRead(testUserId);

        // Assert
        verify(notificationRepository).markAllAsReadByUserId(eq(testUserId), any());
    }

    @Test
    @DisplayName("Phase 5 scaffold: 05-01 may assert preference-backed suppression paths in service helpers")
    void phase5Scaffold_preferenceSuppressionSlot() {
        // Placeholder for service-level preference enforcement checks that depend
        // on the final persisted preference implementation introduced in 05-01.
    }
}
