package com.tradingplatform.security;

import com.tradingplatform.config.JwtConfig;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WebSocket JWT authentication.
 * Tests CHAT-02: WebSocket connection requires valid JWT authentication.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocket Authentication Tests")
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtConfig jwtConfig;

    private JwtTokenProvider jwtTokenProvider;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn("dGVzdC1zZWNyZXQtZm9yLWp3dC10b2tlbi1nZW5lcmF0aW9uLXRlc3QtMjU2LWJpdHMtbG9uZw==");
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);

        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        interceptor = new WebSocketAuthChannelInterceptor(jwtTokenProvider, userRepository);
    }

    @Test
    @DisplayName("Test 1: WebSocket connection succeeds with valid JWT token")
    void testConnectionSucceedsWithValidJwtToken() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedPassword")
                .displayName("Test User")
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtTokenProvider.generateAccessToken(principal);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Create CONNECT message with mutable headers (like Spring does)
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);  // Keep mutable for interceptor
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Assert
        assertNotNull(result);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        Authentication auth = (Authentication) resultAccessor.getUser();
        assertNotNull(auth, "Authentication should be set on successful connection");
        assertTrue(auth.getPrincipal() instanceof UserPrincipal);
        assertEquals(1L, ((UserPrincipal) auth.getPrincipal()).getId());
    }

    @Test
    @DisplayName("Test 2: WebSocket connection fails without Authorization header")
    void testConnectionFailsWithoutAuthorizationHeader() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Assert
        assertNotNull(result);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNull(resultAccessor.getUser(), "Authentication should NOT be set without Authorization header");
    }

    @Test
    @DisplayName("Test 3: WebSocket connection fails with invalid JWT token")
    void testConnectionFailsWithInvalidJwtToken() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid.token.here");
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Assert
        assertNotNull(result);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNull(resultAccessor.getUser(), "Authentication should NOT be set with invalid token");
    }

    @Test
    @DisplayName("Test 4: Non-CONNECT messages pass through unchanged")
    void testNonConnectMessagesPassThrough() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Assert
        assertNotNull(result);
        assertSame(message, result, "Non-CONNECT messages should pass through unchanged");
    }

    @Test
    @DisplayName("Test 5: WebSocket connection fails when user not found")
    void testConnectionFailsWhenUserNotFound() {
        // Arrange
        User user = User.builder()
                .id(999L)
                .email("nonexistent@example.com")
                .password("hashedPassword")
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtTokenProvider.generateAccessToken(principal);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Assert
        assertNotNull(result);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNull(resultAccessor.getUser(), "Authentication should NOT be set when user not found");
    }
}