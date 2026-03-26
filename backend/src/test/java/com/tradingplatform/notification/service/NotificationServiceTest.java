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

import java.time.LocalDateTime;
import java.util.Collections;
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
        when(notificationRepository.findFirstByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalseOrderByCreatedAtDesc(
                anyLong(), any(), anyLong(), anyString()))
                .thenReturn(java.util.Optional.empty());
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
        when(notificationRepository.findFirstByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalseOrderByCreatedAtDesc(
                anyLong(), any(), anyLong(), anyString()))
                .thenReturn(java.util.Optional.empty());
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
        when(notificationRepository.findFirstByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalseOrderByCreatedAtDesc(
                anyLong(), any(), anyLong(), anyString()))
                .thenReturn(java.util.Optional.empty());
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
    @DisplayName("Should lowercase unknown legacy reference types and preserve null values")
    void testCreateNotification_handlesUnknownAndNullReferenceTypes() {
        when(notificationRepository.findFirstByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalseOrderByCreatedAtDesc(
                anyLong(), any(), anyLong(), anyString()))
                .thenReturn(java.util.Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification legacyResult = notificationService.createNotification(
                testUserId,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "Legacy",
                "Legacy reference type",
                400L,
                "Offer_Update"
        );

        Notification nullResult = notificationService.createNotification(
                testUserId,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "No Reference",
                "Null reference type",
                null,
                null
        );

        assertThat(legacyResult.getReferenceType()).isEqualTo("offer_update");
        assertThat(nullResult.getReferenceType()).isNull();
    }

    @Test
    @DisplayName("Should normalize duplicate unread notifications from the same source")
    void testCreateNotification_reusesUnreadNotificationFromSameSource() {
        Notification existing = Notification.builder()
                .id(99L)
                .userId(testUserId)
                .type(NotificationType.NEW_MESSAGE)
                .title("Old title")
                .content("Old content")
                .referenceId(100L)
                .referenceType("conversation")
                .read(false)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(notificationRepository.findFirstByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalseOrderByCreatedAtDesc(
                testUserId, NotificationType.NEW_MESSAGE, 100L, "conversation"))
                .thenReturn(java.util.Optional.of(existing));
        when(notificationRepository.save(existing)).thenReturn(existing);

        Notification result = notificationService.createNotification(
                testUserId,
                NotificationType.NEW_MESSAGE,
                "New title",
                "New content",
                100L,
                "conversation"
        );

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getContent()).isEqualTo("New content");
        verify(notificationRepository, never()).save(argThat(notification -> !notification.equals(existing)));
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
        when(notificationRepository.findByUserIdAndOptionalRead(eq(testUserId), isNull(), any(Pageable.class)))
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
    @DisplayName("Should return filtered notifications for tab all with selected types")
    void testGetNotifications_withAllTabAndTypes() {
        Notification notif1 = Notification.builder()
                .id(1L).userId(testUserId).type(NotificationType.NEW_MESSAGE)
                .title("Message").content("Content1").read(false).build();
        Notification notif2 = Notification.builder()
                .id(2L).userId(testUserId).type(NotificationType.ITEM_SOLD)
                .title("Sold").content("Content2").read(true).build();

        Page<Notification> page = new PageImpl<>(List.of(notif1, notif2));
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        List<NotificationType> types = List.of(NotificationType.NEW_MESSAGE, NotificationType.ITEM_SOLD);

        when(notificationRepository.findByUserIdAndOptionalReadAndTypeIn(
                testUserId, null, types, pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getNotifications(testUserId, "all", types, pageable);

        assertThat(result.getContent()).hasSize(2);
        verify(notificationRepository).findByUserIdAndOptionalReadAndTypeIn(testUserId, null, types, pageable);
    }

    @Test
    @DisplayName("Should return unread notifications when tab unread is requested")
    void testGetNotifications_withUnreadTab() {
        Notification notif = Notification.builder()
                .id(1L).userId(testUserId).type(NotificationType.NEW_MESSAGE)
                .title("Unread").content("Content").read(false).build();

        Page<Notification> page = new PageImpl<>(List.of(notif));
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        when(notificationRepository.findByUserIdAndOptionalRead(
                testUserId, false, pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getNotifications(
                testUserId, "unread", Collections.emptyList(), pageable);

        assertThat(result.getContent()).extracting(NotificationResponse::isRead).containsExactly(false);
        verify(notificationRepository).findByUserIdAndOptionalRead(
                testUserId, false, pageable);
    }

    @Test
    @DisplayName("Should reject unsupported notification tab values")
    void testGetNotifications_withUnsupportedTab_throwsBadRequest() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        assertThatThrownBy(() -> notificationService.getNotifications(testUserId, "archived", List.of(), pageable))
                .isInstanceOf(IllegalArgumentException.class);
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
        when(notificationRepository.findUnreadByUserId(testUserId)).thenReturn(List.of(notif1));

        // Act
        List<NotificationResponse> result = notificationService.getUnreadNotifications(testUserId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("Should cap unread notifications at fifty entries")
    void testGetUnreadNotifications_limitsToFifty() {
        List<Notification> unreadNotifications = java.util.stream.LongStream.rangeClosed(1, 55)
                .mapToObj(id -> Notification.builder()
                        .id(id)
                        .userId(testUserId)
                        .type(NotificationType.NEW_MESSAGE)
                        .title("Title" + id)
                        .content("Content" + id)
                        .read(false)
                        .build())
                .toList();
        when(notificationRepository.findUnreadByUserId(testUserId)).thenReturn(unreadNotifications);

        List<NotificationResponse> result = notificationService.getUnreadNotifications(testUserId);

        assertThat(result).hasSize(50);
    }

    @Test
    @DisplayName("Should mark notification as read (NOTF-04)")
    void testMarkAsRead() {
        // Arrange
        when(notificationRepository.markAsRead(eq(1L), eq(testUserId), any())).thenReturn(1);
        when(notificationRepository.findById(1L)).thenReturn(java.util.Optional.of(
                Notification.builder()
                        .id(1L)
                        .userId(testUserId)
                        .type(NotificationType.NEW_MESSAGE)
                        .title("Test")
                        .content("Unread content")
                        .read(true)
                        .readAt(LocalDateTime.now())
                        .build()
        ));

        // Act
        NotificationResponse response = notificationService.markAsRead(1L, testUserId);

        // Assert
        assertThat(response.getContent()).isEqualTo("Unread content");
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
    @DisplayName("Should mark only visible unread notifications as read for selected types")
    void testMarkVisibleAsRead_withUnreadTabAndTypes() {
        List<NotificationType> types = List.of(NotificationType.NEW_MESSAGE, NotificationType.ITEM_SOLD);
        when(notificationRepository.markVisibleAsReadByType(eq(testUserId), eq(false), eq(types), any())).thenReturn(2);

        notificationService.markVisibleAsRead(testUserId, "unread", types);

        verify(notificationRepository).markVisibleAsReadByType(eq(testUserId), eq(false), eq(types), any());
    }

    @Test
    @DisplayName("Should mark visible notifications as read for tab all without type filter")
    void testMarkVisibleAsRead_withAllTabAndNoTypes() {
        when(notificationRepository.markVisibleAsRead(eq(testUserId), isNull(), any())).thenReturn(3);

        notificationService.markVisibleAsRead(testUserId, "all", Collections.emptyList());

        verify(notificationRepository).markVisibleAsRead(eq(testUserId), isNull(), any());
    }

}
