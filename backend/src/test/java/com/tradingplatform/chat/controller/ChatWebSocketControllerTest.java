package com.tradingplatform.chat.controller;

import com.tradingplatform.chat.dto.*;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.redis.RedisChatEventPublisher;
import com.tradingplatform.chat.service.ChatMessageCommandService;
import com.tradingplatform.chat.service.PresenceService;
import com.tradingplatform.notification.service.NotificationPushService;
import com.tradingplatform.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ChatWebSocketController.
 * Tests for CHAT-02 (real-time messaging) and CHAT-05 (typing indicators).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatWebSocketController Tests")
class ChatWebSocketControllerTest {

    @Mock
    private ChatMessageCommandService chatMessageCommandService;

    @Mock
    private PresenceService presenceService;

    @Mock
    private NotificationPushService notificationPushService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisChatEventPublisher redisChatEventPublisher;

    @InjectMocks
    private ChatWebSocketController controller;

    private Principal principal;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal(1L, "test@example.com", "hashedPassword", Collections.emptyList());
        principal = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            userPrincipal, null, Collections.emptyList());
    }

    @Test
    @DisplayName("Test 1: sendMessage persists message before WebSocket delivery")
    void testSendMessagePersistsBeforeDelivery() {
        // Arrange
        WebSocketMessageRequest request = new WebSocketMessageRequest();
        request.setConversationId(100L);
        request.setContent("Hello");

        MessageResponse messageResponse = MessageResponse.builder()
            .id(1L)
            .conversationId(100L)
            .senderId(1L)
            .content("Hello")
            .status(MessageStatus.PERSISTED)
            .createdAt(java.time.LocalDateTime.of(2026, 3, 30, 12, 0))
            .build();
        MessageAck ack = MessageAck.builder()
            .clientMessageId("client-123")
            .messageId(1L)
            .conversationId(100L)
            .status(MessageStatus.PERSISTED)
            .createdAt(messageResponse.getCreatedAt())
            .build();

        when(chatMessageCommandService.persistMessage(any(ChatMessageCommandService.SendChatMessageCommand.class)))
            .thenReturn(new ChatMessageCommandService.PersistedChatMessage(messageResponse, ack, 2L));

        // Act
        controller.sendMessage(request, principal);

        verify(chatMessageCommandService).persistMessage(any(ChatMessageCommandService.SendChatMessageCommand.class));
        verify(messagingTemplate).convertAndSendToUser(
            eq("1"),
            eq("/queue/message-ack"),
            argThat((MessageAck messageAck) ->
                "client-123".equals(messageAck.getClientMessageId())
                    && Long.valueOf(1L).equals(messageAck.getMessageId())
                    && Long.valueOf(100L).equals(messageAck.getConversationId())
                    && MessageStatus.PERSISTED == messageAck.getStatus()
                    && messageResponse.getCreatedAt().equals(messageAck.getCreatedAt())
            )
        );
        verify(redisChatEventPublisher, never()).publishMessageDelivery(anyLong(), anyLong(), any(MessageResponse.class));
        verify(notificationPushService, never()).pushMessageNotification(anyLong(), any(MessageResponse.class));
    }

    @Test
    @DisplayName("Test 2: sendMessage does not claim recipient delivery in persisted ack path")
    void testSendMessageDoesNotClaimRecipientDelivery() {
        // Arrange
        WebSocketMessageRequest request = new WebSocketMessageRequest();
        request.setConversationId(100L);
        request.setContent("Test message");
        request.setClientMessageId("client-456");

        MessageResponse messageResponse = MessageResponse.builder()
            .id(1L)
            .conversationId(100L)
            .senderId(1L)
            .content("Test message")
            .status(MessageStatus.PERSISTED)
            .createdAt(java.time.LocalDateTime.of(2026, 3, 30, 13, 0))
            .build();
        MessageAck ack = MessageAck.builder()
            .clientMessageId("client-456")
            .messageId(1L)
            .conversationId(100L)
            .status(MessageStatus.PERSISTED)
            .createdAt(messageResponse.getCreatedAt())
            .build();

        when(chatMessageCommandService.persistMessage(any(ChatMessageCommandService.SendChatMessageCommand.class)))
            .thenReturn(new ChatMessageCommandService.PersistedChatMessage(messageResponse, ack, 2L));

        // Act
        controller.sendMessage(request, principal);

        // Assert
        verify(messagingTemplate).convertAndSendToUser(
            eq("1"),
            eq("/queue/message-ack"),
            argThat((MessageAck messageAck) -> MessageStatus.PERSISTED == messageAck.getStatus())
        );
        verify(redisChatEventPublisher, never()).publishMessageDelivery(anyLong(), anyLong(), any(MessageResponse.class));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("2"), eq("/queue/messages"), any(MessageResponse.class));
    }

    @Test
    @DisplayName("Test 3: typing indicator broadcasts to /topic/conversation.{id}.typing")
    void testTypingIndicatorBroadcasts() {
        // Arrange
        TypingRequest request = new TypingRequest(100L, true);

        // Act
        controller.handleTyping(request, principal);

        // Assert
        verify(messagingTemplate).convertAndSend(
            eq("/topic/conversation.100.typing"),
            argThat((TypingResponse response) ->
                response.getUserId().equals(1L) && response.isTyping()
            )
        );
    }

    @Test
    @DisplayName("Test 4: presence updates on connect")
    void testPresenceUpdatesOnConnect() {
        // Arrange - simulate a WebSocket connect event
        var event = mock(org.springframework.web.socket.messaging.SessionConnectedEvent.class);
        var message = mock(org.springframework.messaging.Message.class);
        var accessor = org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(
            org.springframework.messaging.simp.stomp.StompCommand.CONNECT);

        accessor.setUser((org.springframework.security.core.Authentication) principal);
        accessor.setSessionId("session-1");
        when(event.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
        when(presenceService.userConnected(1L, "session-1")).thenReturn(true);

        // Act
        controller.handleWebSocketConnectListener(event);

        // Assert
        verify(presenceService).userConnected(1L, "session-1");
        verify(notificationPushService).pushSellerOnlineNotifications(1L);
        verify(redisChatEventPublisher).publishPresenceUpdate(
            argThat((PresenceUpdateResponse response) ->
                response.getUserId().equals(1L) && response.isOnline()
            )
        );
    }

    @Test
    @DisplayName("Test 5: presence updates on disconnect")
    void testPresenceUpdatesOnDisconnect() {
        // Arrange
        var event = mock(org.springframework.web.socket.messaging.SessionDisconnectEvent.class);
        var accessor = org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(
            org.springframework.messaging.simp.stomp.StompCommand.DISCONNECT);

        accessor.setUser((org.springframework.security.core.Authentication) principal);
        accessor.setSessionId("session-1");
        when(event.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
        when(presenceService.userDisconnected(1L, "session-1")).thenReturn(true);

        // Act
        controller.handleWebSocketDisconnectListener(event);

        // Assert
        verify(presenceService).userDisconnected(1L, "session-1");
        verify(redisChatEventPublisher).publishPresenceUpdate(
            argThat((PresenceUpdateResponse response) ->
                response.getUserId().equals(1L) && !response.isOnline()
            )
        );
    }

    @Test
    @DisplayName("Test 6: heartbeat updates presence")
    void testHeartbeatUpdatesPresence() {
        // Act
        controller.handleHeartbeat(principal, "session-1");

        // Assert
        verify(presenceService).heartbeat(1L, "session-1");
    }

    @Test
    @DisplayName("Test 7: empty message is ignored")
    void testEmptyMessageIgnored() {
        // Arrange
        WebSocketMessageRequest request = new WebSocketMessageRequest();
        request.setConversationId(100L);
        request.setContent(""); // Empty content

        // Act
        controller.sendMessage(request, principal);

        // Assert
        verify(chatMessageCommandService, never()).persistMessage(any(ChatMessageCommandService.SendChatMessageCommand.class));
    }
}
