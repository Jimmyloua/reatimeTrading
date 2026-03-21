package com.tradingplatform.security;

import com.tradingplatform.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("VGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIEpXVCAyNTYgYml0cw==");
        jwtConfig.setAccessTokenExpiration(900000L); // 15 minutes
        jwtConfig.setRefreshTokenExpiration(604800000L); // 7 days

        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    private UserPrincipal createUserPrincipal(Long id, String email) {
        return UserPrincipal.builder()
                .id(id)
                .email(email)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("generateAccessToken returns non-null string")
    void generateAccessToken_returnsNonNullString() {
        UserPrincipal userPrincipal = createUserPrincipal(1L, "test@example.com");

        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("generateRefreshToken returns non-null string")
    void generateRefreshToken_returnsNonNullString() {
        UserPrincipal userPrincipal = createUserPrincipal(1L, "test@example.com");

        String token = jwtTokenProvider.generateRefreshToken(userPrincipal);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("validateToken returns true for valid token")
    void validateToken_returnsTrueForValidToken() {
        UserPrincipal userPrincipal = createUserPrincipal(1L, "test@example.com");

        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for invalid token")
    void validateToken_returnsFalseForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void validateToken_returnsFalseForExpiredToken() throws InterruptedException {
        // Create config with very short expiration (1 second)
        JwtConfig shortConfig = new JwtConfig();
        shortConfig.setSecret("VGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIEpXVCAyNTYgYml0cw==");
        shortConfig.setAccessTokenExpiration(1000L); // 1 second
        shortConfig.setRefreshTokenExpiration(1000L);

        JwtTokenProvider shortProvider = new JwtTokenProvider(shortConfig);

        UserPrincipal userPrincipal = createUserPrincipal(1L, "test@example.com");

        String token = shortProvider.generateAccessToken(userPrincipal);

        // Wait for token to expire
        Thread.sleep(1500);

        assertFalse(shortProvider.validateToken(token));
    }

    @Test
    @DisplayName("getUserIdFromToken returns correct user ID")
    void getUserIdFromToken_returnsCorrectUserId() {
        UserPrincipal userPrincipal = createUserPrincipal(123L, "test@example.com");

        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        String userId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals("123", userId);
    }

    @Test
    @DisplayName("getEmailFromToken returns correct email")
    void getEmailFromToken_returnsCorrectEmail() {
        UserPrincipal userPrincipal = createUserPrincipal(1L, "testuser@example.com");

        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        String email = jwtTokenProvider.getEmailFromToken(token);

        assertEquals("testuser@example.com", email);
    }
}