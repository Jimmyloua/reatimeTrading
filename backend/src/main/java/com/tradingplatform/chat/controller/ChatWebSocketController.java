package com.tradingplatform.chat.controller;

import com.tradingplatform.chat.dto.*;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.service.ChatService;
import com.tradingplatform.chat.service.PresenceService;
import com.tradingplatform.notification.service.NotificationPushService;
import com.tradingplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * WebSocket controller for real-time chat messaging.
 * Implements CHAT-02: Real-time messages.
 * Implements CHAT-05: Typing indicators.
 * Implements D-09: Typing indicator shown in active conversations.
 * Implements D-10: Online/offline presence shown per user.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final PresenceService presenceService;
    private final NotificationPushService notificationPushService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles real-time message sending.
     * CRITICAL: Persists to database FIRST (per ROADMAP note) before delivery.
     * Implements CHAT-02.
     *
     * @param request the message request
     * @param principal the authenticated user
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload WebSocketMessageRequest request, Principal principal) {
        Long senderId = getUserId(principal);
        if (senderId == null) return;

        if (!request.hasContent()) {
            log.warn("Empty message from user {}", senderId);
            return;
        }

        // CRITICAL: Persist to database FIRST (per ROADMAP critical note)
        MessageResponse message = chatService.sendMessage(
            request.getConversationId(),
            senderId,
            request.getContent(),
            request.getImageUrl()
        );

        // Deliver to recipient via WebSocket
        Long recipientId = chatService.getOtherParticipantId(request.getConversationId(), senderId);
        if (recipientId != null) {
            messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                message
            );

            // Push notification to recipient
            notificationPushService.pushMessageNotification(recipientId, message);
        }

        // Send ACK back to sender
        messagingTemplate.convertAndSendToUser(
            senderId.toString(),
            "/queue/message-ack",
            MessageAck.builder()
                .messageId(message.getId())
                .status(MessageStatus.DELIVERED)
                .build()
        );
    }

    /**
     * Handles typing indicator.
     * Implements CHAT-05.
     *
     * @param request the typing request
     * @param principal the authenticated user
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingRequest request, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return;

        // Broadcast typing status to conversation participants
        messagingTemplate.convertAndSend(
            "/topic/conversation." + request.getConversationId() + ".typing",
            TypingResponse.builder()
                .userId(userId)
                .typing(request.isTyping())
                .build()
        );
    }

    /**
     * Handles presence heartbeat.
     *
     * @param principal the authenticated user
     */
    @MessageMapping("/chat.heartbeat")
    public void handleHeartbeat(Principal principal) {
        Long userId = getUserId(principal);
        if (userId != null) {
            presenceService.heartbeat(userId);
        }
    }

    /**
     * Event listener for WebSocket connection.
     * Updates presence status on connect.
     *
     * @param event the session connected event
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) accessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            presenceService.userConnected(principal.getId());
            log.info("User {} connected via WebSocket", principal.getId());
        }
    }

    /**
     * Event listener for WebSocket disconnection.
     * Updates presence status on disconnect.
     *
     * @param event the session disconnect event
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) accessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            presenceService.userDisconnected(principal.getId());
            log.info("User {} disconnected from WebSocket", principal.getId());
        }
    }

    /**
     * Extracts the user ID from the principal.
     *
     * @param principal the principal
     * @return the user ID or null if not authenticated
     */
    private Long getUserId(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return null;
    }
}