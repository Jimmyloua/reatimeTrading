package com.tradingplatform.avatar;

import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;
    private final UserRepository userRepository;

    /**
     * Uploads an avatar image for the current user.
     * Implements PROF-02: User can upload avatar image.
     * Implements D-09: Avatar stored on local filesystem.
     * Implements D-10: Max 5 MB file size.
     * Implements D-11: JPEG, PNG, WebP only.
     * Implements D-12: Replaces existing avatar.
     */
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {

        // Store the avatar
        String filename = avatarService.storeAvatar(file, principal.getId());

        // Update user's avatar path
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatarPath(filename);
        userRepository.save(user);

        String avatarUrl = "/uploads/avatars/" + filename;
        log.info("Avatar uploaded for user {}: {}", principal.getId(), avatarUrl);

        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    /**
     * Deletes the avatar for the current user.
     */
    @DeleteMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAvatarPath() != null) {
            avatarService.deleteAvatar(user.getAvatarPath());
            user.setAvatarPath(null);
            userRepository.save(user);
            log.info("Avatar deleted for user {}", principal.getId());
        }

        return ResponseEntity.noContent().build();
    }
}