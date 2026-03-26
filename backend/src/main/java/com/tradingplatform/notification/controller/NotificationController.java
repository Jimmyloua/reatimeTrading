package com.tradingplatform.notification.controller;

import com.tradingplatform.notification.dto.NotificationResponse;
import com.tradingplatform.notification.dto.NotificationPreferenceResponse;
import com.tradingplatform.notification.dto.UpdateNotificationPreferencesRequest;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.service.NotificationPreferenceService;
import com.tradingplatform.notification.service.NotificationService;
import com.tradingplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for notification operations.
 * Implements NOTF-03 and NOTF-04.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationPreferenceService notificationPreferenceService;

    /**
     * Gets paginated notifications for the current user.
     * Implements NOTF-03: User can view notification history.
     *
     * @param principal the authenticated user
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated notifications
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(required = false) List<NotificationType> types,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getNotifications(principal.getId(), tab, types, pageable));
    }

    /**
     * Gets unread notifications for the current user.
     * Returns up to 50 unread notifications per D-13.
     *
     * @param principal the authenticated user
     * @return list of unread notifications
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(principal.getId()));
    }

    /**
     * Gets unread notification count for the current user.
     *
     * @param principal the authenticated user
     * @return map with unread count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", notificationService.getUnreadCount(principal.getId()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationPreferenceService.getPreferences(principal.getId()));
    }

    @PatchMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        return ResponseEntity.ok(notificationPreferenceService.updatePreferences(principal.getId(), request));
    }

    /**
     * Marks a single notification as read.
     * Implements NOTF-04: User can mark notifications as read.
     *
     * @param id the notification ID
     * @param principal the authenticated user
     * @return 200 OK on success
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markAsRead(id, principal.getId()));
    }

    /**
     * Marks all notifications as read for the current user.
     * Implements NOTF-04: User can mark all notifications as read.
     *
     * @param principal the authenticated user
     * @return 200 OK on success
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-visible")
    public ResponseEntity<Void> markVisibleAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(required = false) List<NotificationType> types) {
        notificationService.markVisibleAsRead(principal.getId(), tab, types);
        return ResponseEntity.ok().build();
    }
}
