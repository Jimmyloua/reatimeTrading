package com.tradingplatform.notification.dto;

import com.tradingplatform.notification.entity.Notification;
import com.tradingplatform.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for notification response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private Long referenceId;
    private String referenceType;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    /**
     * Converts a Notification entity to NotificationResponse DTO.
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .read(notification.getRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}