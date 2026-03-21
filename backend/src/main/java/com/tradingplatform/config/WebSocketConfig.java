package com.tradingplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for real-time communication.
 * Implements STOMP protocol with SockJS fallback.
 * Supports CHAT-02, CHAT-05, NOTF-01, NOTF-02.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for /topic (broadcast) and /queue (user-specific)
        // In production, this would be replaced with Redis-backed broker for clustering
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
            .withSockJS();  // Fallback support for restricted networks
    }
}