package com.tradingplatform.notification.service;

import com.tradingplatform.notification.dto.NotificationPreferenceResponse;
import com.tradingplatform.notification.dto.UpdateNotificationPreferencesRequest;
import com.tradingplatform.notification.entity.NotificationPreference;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(Long userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .map(NotificationPreferenceResponse::from)
                .orElseGet(NotificationPreferenceResponse::defaults);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(Long userId, UpdateNotificationPreferencesRequest request) {
        NotificationPreference preference = notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> NotificationPreference.builder().userId(userId).build());

        if (request.getNewMessageEnabled() != null) {
            preference.setNewMessageEnabled(request.getNewMessageEnabled());
        }
        if (request.getItemSoldEnabled() != null) {
            preference.setItemSoldEnabled(request.getItemSoldEnabled());
        }
        if (request.getTransactionUpdateEnabled() != null) {
            preference.setTransactionUpdateEnabled(request.getTransactionUpdateEnabled());
        }

        return NotificationPreferenceResponse.from(notificationPreferenceRepository.save(preference));
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId, NotificationType type) {
        NotificationPreferenceResponse preferences = getPreferences(userId);
        return switch (type) {
            case NEW_MESSAGE, SELLER_ONLINE -> preferences.isNewMessageEnabled();
            case ITEM_SOLD -> preferences.isItemSoldEnabled();
            case TRANSACTION_UPDATE -> preferences.isTransactionUpdateEnabled();
            default -> true;
        };
    }
}
