package com.tradingplatform.security;

import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Channel interceptor for WebSocket JWT authentication.
 * Validates JWT tokens on STOMP CONNECT frames.
 * Implements CHAT-02: WebSocket connection requires valid JWT authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtTokenProvider.validateToken(token)) {
                    try {
                        String userId = jwtTokenProvider.getUserIdFromToken(token);
                        User user = userRepository.findById(Long.parseLong(userId)).orElse(null);

                        if (user != null) {
                            UserPrincipal principal = new UserPrincipal(
                                user.getId(),
                                user.getEmail(),
                                user.getPassword(),
                                Collections.emptyList()
                            );
                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                principal, null, Collections.emptyList());
                            accessor.setUser(auth);
                            log.debug("WebSocket authenticated for user: {}", userId);
                        }
                    } catch (Exception e) {
                        log.warn("WebSocket authentication failed: {}", e.getMessage());
                    }
                }
            }
        }

        return message;
    }
}