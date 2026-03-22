package com.tradingplatform.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_preferences_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "new_message_enabled", nullable = false)
    @Builder.Default
    private Boolean newMessageEnabled = true;

    @Column(name = "item_sold_enabled", nullable = false)
    @Builder.Default
    private Boolean itemSoldEnabled = true;

    @Column(name = "transaction_update_enabled", nullable = false)
    @Builder.Default
    private Boolean transactionUpdateEnabled = true;
}
