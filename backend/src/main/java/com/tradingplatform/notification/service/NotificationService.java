package com.tradingplatform.notification.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.notification.dto.NotificationResponse;
import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service for notification operations.
 * Implements NOTF-01 to NOTF-04 requirements.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    /**
     * Creates a new notification.
     *
     * @param userId the recipient user ID
     * @param type the notification type
     * @param title the notification title
     * @param content the notification content
     * @param referenceId optional reference ID (conversation, listing, transaction)
     * @param referenceType optional reference type
     * @return the created notification
     */
    @Transactional
    public Notification createNotification(Long userId, NotificationType type,
                                           String title, String content,
                                           Long referenceId, String referenceType) {
        String normalizedReferenceType = normalizeReferenceType(referenceType);

        if (referenceId != null && normalizedReferenceType != null) {
            Notification existingNotification = notificationRepository
                .findFirstByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalseOrderByCreatedAtDesc(
                    userId,
                    type,
                    referenceId,
                    normalizedReferenceType
                )
                .orElse(null);

            if (existingNotification != null) {
                existingNotification.setTitle(title);
                existingNotification.setContent(content);
                existingNotification.setRead(false);
                existingNotification.setReadAt(null);
                return notificationRepository.save(existingNotification);
            }
        }

        Notification notification = Notification.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .content(content)
            .referenceId(referenceId)
            .referenceType(normalizedReferenceType)
            .read(false)
            .build();

        return notificationRepository.save(notification);
    }

    /**
     * Gets paginated notifications for a user.
     * Implements NOTF-03: User can view notification history.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return paginated notification responses
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return getNotifications(userId, "all", Collections.emptyList(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(
            Long userId,
            String tab,
            List<NotificationType> types,
            Pageable pageable) {
        Boolean readState = resolveReadState(tab);
        List<NotificationType> requestedTypes = normalizeTypes(types);

        Page<Notification> notifications = requestedTypes.isEmpty()
                ? notificationRepository.findByUserIdAndOptionalRead(userId, readState, pageable)
                : notificationRepository.findByUserIdAndOptionalReadAndTypeIn(userId, readState, requestedTypes, pageable);

        return notifications
            .map(this::toResponse);
    }

    /**
     * Gets unread notification count for a user.
     *
     * @param userId the user ID
     * @return the unread count
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Gets up to 50 unread notifications for a user.
     * Per D-13: Notification history limited to 50 unread.
     *
     * @param userId the user ID
     * @return list of unread notifications
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
            .limit(50)
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Marks a single notification as read.
     * Implements NOTF-04: User can mark notifications as read.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for authorization)
     * @throws ApiException if notification not found
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        if (updated == 0) {
            throw new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        Notification notification = notificationRepository.findById(notificationId)
            .filter(current -> current.getUserId().equals(userId))
            .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));

        return toResponse(notification);
    }

    /**
     * Marks all notifications as read for a user.
     * Implements NOTF-04: User can mark all notifications as read.
     *
     * @param userId the user ID
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public void markVisibleAsRead(Long userId, String tab, List<NotificationType> types) {
        Boolean readState = resolveReadState(tab);
        List<NotificationType> requestedTypes = normalizeTypes(types);
        LocalDateTime readAt = LocalDateTime.now();

        if (requestedTypes.isEmpty()) {
            notificationRepository.markVisibleAsRead(userId, readState, readAt);
            return;
        }

        notificationRepository.markVisibleAsReadByType(userId, readState, requestedTypes, readAt);
    }

    /**
     * Converts a Notification entity to NotificationResponse DTO.
     */
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.from(notification);
    }

    private String normalizeReferenceType(String referenceType) {
        if (referenceType == null) {
            return null;
        }
        return referenceType.trim().toLowerCase(Locale.ROOT);
    }

    private Boolean resolveReadState(String tab) {
        if (tab == null || tab.isBlank() || "all".equalsIgnoreCase(tab)) {
            return null;
        }
        if ("unread".equalsIgnoreCase(tab)) {
            return false;
        }
        throw new IllegalArgumentException("Unsupported notification tab: " + tab);
    }

    private List<NotificationType> normalizeTypes(List<NotificationType> types) {
        if (types == null || types.isEmpty()) {
            return Collections.emptyList();
        }
        return types;
    }
}
