package com.tradingplatform.notification.service;

import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.notification.dto.NotificationResponse;
import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for NotificationPushService.
 * Tests NOTF-01 (message notification) and NOTF-02 (item sold notification).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationPushService Tests")
class NotificationPushServiceTest {

    /**
     * Phase 5 Wave 0 note:
     * 05-00 keeps push-path coverage green and reserves explicit slots for 05-01
     * to harden suppression and preference fan-out assertions.
     */

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationPushService pushService;

    @Test
    @DisplayName("Test 1: pushMessageNotification creates notification and sends via WebSocket")
    void testPushMessageNotification() {
        // Arrange
        Long recipientId = 2L;
        MessageResponse message = MessageResponse.builder()
            .id(1L)
            .conversationId(100L)
            .senderId(1L)
            .senderName("John")
            .content("Hello")
            .build();

        Notification notification = Notification.builder()
            .id(1L)
            .userId(recipientId)
            .type(NotificationType.NEW_MESSAGE)
            .title("New Message")
            .content("John sent you a message")
            .referenceId(100L)
            .referenceType("conversation")
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();

        when(notificationService.createNotification(
            eq(recipientId),
            eq(NotificationType.NEW_MESSAGE),
            anyString(),
            anyString(),
            eq(100L),
            eq("conversation")
        )).thenReturn(notification);
        when(notificationPreferenceService.isEnabled(recipientId, NotificationType.NEW_MESSAGE)).thenReturn(true);

        // Act
        pushService.pushMessageNotification(recipientId, message);

        // Assert
        verify(notificationService).createNotification(
            eq(recipientId),
            eq(NotificationType.NEW_MESSAGE),
            eq("New Message"),
            eq("John sent you a message"),
            eq(100L),
            eq("conversation")
        );

        // Verify WebSocket push
        verify(messagingTemplate).convertAndSendToUser(
            eq(recipientId.toString()),
            eq("/queue/notifications"),
            any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("Test 2: pushItemSoldNotification creates ITEM_SOLD notification")
    void testPushItemSoldNotification() {
        // Arrange
        Long sellerId = 1L;
        Long listingId = 200L;
        String listingTitle = "iPhone 15 Pro Max";

        Notification notification = Notification.builder()
            .id(1L)
            .userId(sellerId)
            .type(NotificationType.ITEM_SOLD)
            .title("Item Sold")
            .content("Your item 'iPhone 15 Pro Max' was marked as sold")
            .referenceId(listingId)
            .referenceType("listing")
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();

        when(notificationService.createNotification(
            eq(sellerId),
            eq(NotificationType.ITEM_SOLD),
            anyString(),
            anyString(),
            eq(listingId),
            eq("listing")
        )).thenReturn(notification);
        when(notificationPreferenceService.isEnabled(sellerId, NotificationType.ITEM_SOLD)).thenReturn(true);

        // Act
        pushService.pushItemSoldNotification(sellerId, listingId, listingTitle);

        // Assert
        verify(notificationService).createNotification(
            eq(sellerId),
            eq(NotificationType.ITEM_SOLD),
            eq("Item Sold"),
            contains("iPhone 15 Pro Max"),
            eq(listingId),
            eq("listing")
        );

        // Verify WebSocket push
        verify(messagingTemplate).convertAndSendToUser(
            eq(sellerId.toString()),
            eq("/queue/notifications"),
            any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("Test 3: Notification is delivered to correct user via /user/queue/notifications")
    void testNotificationDeliveredToCorrectUser() {
        // Arrange
        Long userId = 42L;

        Notification notification = Notification.builder()
            .id(1L)
            .userId(userId)
            .type(NotificationType.TRANSACTION_UPDATE)
            .title("Transaction Update")
            .content("Transaction status: completed")
            .referenceId(300L)
            .referenceType("transaction")
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();

        when(notificationService.createNotification(
            eq(userId),
            eq(NotificationType.TRANSACTION_UPDATE),
            anyString(),
            anyString(),
            anyLong(),
            anyString()
        )).thenReturn(notification);
        when(notificationPreferenceService.isEnabled(userId, NotificationType.TRANSACTION_UPDATE)).thenReturn(true);

        // Act
        pushService.pushTransactionNotification(userId, 300L, "completed");

        // Assert
        verify(messagingTemplate).convertAndSendToUser(
            eq("42"),  // userId as string
            eq("/queue/notifications"),
            argThat((NotificationResponse response) ->
                response.getType() == NotificationType.TRANSACTION_UPDATE &&
                response.getTitle().equals("Transaction Update")
            )
        );
    }

    @Test
    @DisplayName("Test 4: Long listing title is truncated in item sold notification")
    void testLongTitleTruncated() {
        // Arrange
        Long sellerId = 1L;
        Long listingId = 200L;
        String longTitle = "This is a very long listing title that should be truncated because it exceeds fifty characters";

        Notification notification = Notification.builder()
            .id(1L)
            .userId(sellerId)
            .type(NotificationType.ITEM_SOLD)
            .title("Item Sold")
            .content("Your item 'This is a very long listing title that should be ...' was marked as sold")
            .referenceId(listingId)
            .referenceType("listing")
            .build();

        when(notificationService.createNotification(anyLong(), any(), anyString(), anyString(), anyLong(), anyString()))
            .thenReturn(notification);
        when(notificationPreferenceService.isEnabled(sellerId, NotificationType.ITEM_SOLD)).thenReturn(true);

        // Act
        pushService.pushItemSoldNotification(sellerId, listingId, longTitle);

        // Assert
        verify(notificationService).createNotification(
            eq(sellerId),
            eq(NotificationType.ITEM_SOLD),
            eq("Item Sold"),
            argThat(content -> content.length() < 120 && content.contains("...")),
            eq(listingId),
            eq("listing")
        );
    }

    @Test
    @DisplayName("Test 5: Disabled NEW_MESSAGE preference suppresses creation and push")
    void testPushMessageNotification_suppressedWhenDisabled() {
        Long recipientId = 9L;
        MessageResponse message = MessageResponse.builder()
                .conversationId(55L)
                .senderName("John")
                .build();

        when(notificationPreferenceService.isEnabled(recipientId, NotificationType.NEW_MESSAGE)).thenReturn(false);

        pushService.pushMessageNotification(recipientId, message);

        verify(notificationService, never()).createNotification(anyLong(), any(), anyString(), anyString(), any(), any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Test 6: Disabled ITEM_SOLD preference suppresses creation and push")
    void testPushItemSoldNotification_suppressedWhenDisabled() {
        Long sellerId = 4L;

        when(notificationPreferenceService.isEnabled(sellerId, NotificationType.ITEM_SOLD)).thenReturn(false);

        pushService.pushItemSoldNotification(sellerId, 88L, "Camera");

        verify(notificationService, never()).createNotification(anyLong(), any(), anyString(), anyString(), any(), any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Test 7: Disabled TRANSACTION_UPDATE preference suppresses creation and push only")
    void testPushTransactionNotification_suppressedWhenDisabled() {
        Long userId = 7L;

        when(notificationPreferenceService.isEnabled(userId, NotificationType.TRANSACTION_UPDATE)).thenReturn(false);

        pushService.pushTransactionNotification(userId, 300L, "completed");

        verify(notificationService, never()).createNotification(anyLong(), any(), anyString(), anyString(), any(), any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Test 8: Transaction notification keeps canonical transaction reference when enabled")
    void testPushTransactionNotification_usesCanonicalTransactionReference() {
        Long userId = 42L;
        Notification notification = Notification.builder()
                .id(10L)
                .userId(userId)
                .type(NotificationType.TRANSACTION_UPDATE)
                .title("Transaction Update")
                .content("Transaction status: shipped")
                .referenceId(300L)
                .referenceType("transaction")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationPreferenceService.isEnabled(userId, NotificationType.TRANSACTION_UPDATE)).thenReturn(true);
        when(notificationService.createNotification(
                eq(userId),
                eq(NotificationType.TRANSACTION_UPDATE),
                anyString(),
                anyString(),
                eq(300L),
                eq("transaction")
        )).thenReturn(notification);

        pushService.pushTransactionNotification(userId, 300L, "shipped");

        verify(notificationService).createNotification(
                eq(userId),
                eq(NotificationType.TRANSACTION_UPDATE),
                eq("Transaction Update"),
                eq("Transaction status: shipped"),
                eq(300L),
                eq("transaction")
        );
        verify(messagingTemplate).convertAndSendToUser(
                eq("42"),
                eq("/queue/notifications"),
                argThat((NotificationResponse response) -> {
                    assertThat(response.getReferenceType()).isEqualTo("transaction");
                    assertThat(response.getReferenceId()).isEqualTo(300L);
                    return true;
                })
        );
    }
}
