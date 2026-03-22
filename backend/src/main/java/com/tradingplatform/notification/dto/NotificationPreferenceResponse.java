package com.tradingplatform.notification.dto;

import com.tradingplatform.notification.entity.NotificationPreference;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceResponse {
    private boolean newMessageEnabled;
    private boolean itemSoldEnabled;
    private boolean transactionUpdateEnabled;

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .newMessageEnabled(Boolean.TRUE.equals(preference.getNewMessageEnabled()))
                .itemSoldEnabled(Boolean.TRUE.equals(preference.getItemSoldEnabled()))
                .transactionUpdateEnabled(Boolean.TRUE.equals(preference.getTransactionUpdateEnabled()))
                .build();
    }

    public static NotificationPreferenceResponse defaults() {
        return NotificationPreferenceResponse.builder()
                .newMessageEnabled(true)
                .itemSoldEnabled(true)
                .transactionUpdateEnabled(true)
                .build();
    }
}
