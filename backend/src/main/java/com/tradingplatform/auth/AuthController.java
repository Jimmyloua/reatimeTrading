package com.tradingplatform.auth;

import com.tradingplatform.auth.dto.LoginRequest;
import com.tradingplatform.auth.dto.LoginResponse;
import com.tradingplatform.auth.dto.LogoutRequest;
import com.tradingplatform.auth.dto.RefreshTokenRequest;
import com.tradingplatform.auth.dto.RegisterRequest;
import com.tradingplatform.config.JwtConfig;
import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.security.JwtTokenProvider;
import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final JwtConfig jwtConfig;

    /**
     * Registers a new user and returns JWT tokens.
     * Implements D-01: Immediate access after registration.
     * Implements D-02: Registration collects only email and password.
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.getEmail(), request.getPassword());

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshToken = tokenProvider.generateRefreshToken(principal);

        // Store refresh token hash for validation during refresh
        String refreshTokenHash = hashToken(refreshToken);
        userService.updateRefreshTokenHash(user.getId(), refreshTokenHash);

        return ResponseEntity.ok(buildLoginResponse(accessToken, refreshToken, user.getId()));
    }

    /**
     * Authenticates user and returns JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = userService.findById(principal.getId());

            String accessToken = tokenProvider.generateAccessToken(principal);
            String refreshToken = tokenProvider.generateRefreshToken(principal);

            // Store refresh token hash for validation during refresh
            String refreshTokenHash = hashToken(refreshToken);
            userService.updateRefreshTokenHash(user.getId(), refreshTokenHash);

            return ResponseEntity.ok(buildLoginResponse(accessToken, refreshToken, user.getId()));
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for email: {}", request.getEmail());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * Logs out user by invalidating refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            return ResponseEntity.ok().build();
        }

        String userId = tokenProvider.getUserIdFromToken(request.getRefreshToken());
        userService.clearRefreshTokenHash(Long.parseLong(userId));

        log.info("User {} logged out", userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Refreshes access token using refresh token.
     * Implements refresh token rotation for security.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Invalid or expired refresh token");
        }

        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userService.findById(Long.parseLong(userId));

        // Verify stored refresh token hash matches (prevents reuse after logout)
        String refreshTokenHash = hashToken(refreshToken);
        if (user.getRefreshTokenHash() == null || !user.getRefreshTokenHash().equals(refreshTokenHash)) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Refresh token has been revoked");
        }

        UserPrincipal principal = UserPrincipal.create(user);

        // Generate new tokens (rotation)
        String newAccessToken = tokenProvider.generateAccessToken(principal);
        String newRefreshToken = tokenProvider.generateRefreshToken(principal);

        // Update refresh token hash
        String newRefreshTokenHash = hashToken(newRefreshToken);
        userService.updateRefreshTokenHash(user.getId(), newRefreshTokenHash);

        return ResponseEntity.ok(buildLoginResponse(newAccessToken, newRefreshToken, user.getId()));
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, Long userId) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .expiresIn(jwtConfig.getAccessTokenExpiration() / 1000) // Convert to seconds
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}