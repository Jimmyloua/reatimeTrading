package com.tradingplatform.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;

    private String displayName;

    private String avatarUrl;

    private LocalDateTime createdAt;

    private Long listingCount;

    private boolean ownProfile;

    @JsonProperty("isOwnProfile")
    public boolean isOwnProfile() {
        return ownProfile;
    }

    public void setOwnProfile(boolean ownProfile) {
        this.ownProfile = ownProfile;
    }
}