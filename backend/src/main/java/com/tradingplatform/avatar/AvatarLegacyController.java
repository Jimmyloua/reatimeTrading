package com.tradingplatform.avatar;

import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AvatarLegacyController {

    private final AvatarService avatarService;
    private final UserRepository userRepository;

    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Map<String, String>>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return Mono.fromCallable(() -> uploadAvatarBlocking(principal, file))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Void>> deleteAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        return Mono.fromCallable(() -> deleteAvatarBlocking(principal))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ResponseEntity<Map<String, String>> uploadAvatarBlocking(UserPrincipal principal, MultipartFile file) {
        String filename = avatarService.storeAvatar(file, principal.getId());

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatarPath(filename);
        userRepository.save(user);

        String avatarUrl = "/uploads/avatars/" + filename;
        log.info("Avatar uploaded for user {}: {}", principal.getId(), avatarUrl);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    private ResponseEntity<Void> deleteAvatarBlocking(UserPrincipal principal) {
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
