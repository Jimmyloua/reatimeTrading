package com.tradingplatform.notification.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationPreferencesRequest {
    private Boolean newMessageEnabled;
    private Boolean itemSoldEnabled;
    private Boolean transactionUpdateEnabled;
}
