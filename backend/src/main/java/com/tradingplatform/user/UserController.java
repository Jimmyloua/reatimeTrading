package com.tradingplatform.user;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.user.dto.UpdateProfileRequest;
import com.tradingplatform.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Get the current authenticated user's profile.
     * Implements PROF-03: User can view their own profile.
     */
    @GetMapping("/me")
    @PreAuthorize("isFullyAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(buildProfileResponse(user, true));
    }

    /**
     * Update the current authenticated user's profile.
     * Implements PROF-01: User can set display name.
     * Implements D-06: profileComplete is updated when display name is set.
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // Update display name
        user.setDisplayName(request.getDisplayName());

        // Update profileComplete flag based on display name
        user.updateProfileComplete();

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(buildProfileResponse(savedUser, true));
    }

    /**
     * Get a public profile for any user by ID.
     * Implements PROF-04: User can view other users' profiles.
     * This endpoint can be accessed without authentication.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // Determine if this is the user's own profile
        boolean isOwnProfile = principal != null && principal.getId().equals(id);

        return ResponseEntity.ok(buildProfileResponse(user, isOwnProfile));
    }

    /**
     * Builds a UserProfileResponse from a User entity.
     * Implements D-08: Users without display name show "New User" placeholder.
     */
    private UserProfileResponse buildProfileResponse(User user, boolean isOwnProfile) {
        String avatarUrl = user.getAvatarPath() != null
                ? "/uploads/avatars/" + user.getAvatarPath()
                : null;

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(isOwnProfile ? user.getEmail() : null)
                .displayName(user.getDisplayNameOrFallback()) // D-08: "New User" fallback
                .avatarUrl(avatarUrl)
                .createdAt(user.getCreatedAt())
                .listingCount(0L) // Will be joined in Phase 2
                .ownProfile(isOwnProfile)
                .build();
    }
}