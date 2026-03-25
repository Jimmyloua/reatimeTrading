package com.tradingplatform.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Presence update payload for real-time seller/buyer availability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceUpdateResponse {

    private Long userId;
    private boolean online;
    private String lastSeenText;
    private LocalDateTime updatedAt;
}
