package com.tradingplatform.chat.controller;

import com.tradingplatform.chat.dto.*;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.service.ChatService;
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
    private ChatService chatService;

    @Mock
    private PresenceService presenceService;

    @Mock
    private NotificationPushService notificationPushService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

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
            .status(MessageStatus.SENT)
            .build();

        when(chatService.sendMessage(100L, 1L, "Hello", null)).thenReturn(messageResponse);
        when(chatService.getOtherParticipantId(100L, 1L)).thenReturn(2L);

        // Act
        controller.sendMessage(request, principal);

        // Assert - verify persistence happened first (this is the critical ROADMAP requirement)
        verify(chatService).sendMessage(100L, 1L, "Hello", null);
        verify(chatService).getOtherParticipantId(100L, 1L);

        // Verify delivery to recipient
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/messages"), any(MessageResponse.class));

        // Verify ACK sent to sender
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/message-ack"), any(MessageAck.class));

        // Verify notification pushed
        verify(notificationPushService).pushMessageNotification(eq(2L), any(MessageResponse.class));
    }

    @Test
    @DisplayName("Test 2: sendMessage delivers to recipient via /user/queue/messages")
    void testSendMessageDeliversToRecipient() {
        // Arrange
        WebSocketMessageRequest request = new WebSocketMessageRequest();
        request.setConversationId(100L);
        request.setContent("Test message");

        MessageResponse messageResponse = MessageResponse.builder()
            .id(1L)
            .conversationId(100L)
            .senderId(1L)
            .content("Test message")
            .build();

        when(chatService.sendMessage(anyLong(), anyLong(), anyString(), isNull())).thenReturn(messageResponse);
        when(chatService.getOtherParticipantId(100L, 1L)).thenReturn(2L);

        // Act
        controller.sendMessage(request, principal);

        // Assert
        verify(messagingTemplate).convertAndSendToUser(
            eq("2"),
            eq("/queue/messages"),
            any(MessageResponse.class)
        );
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
        when(event.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

        // Act
        controller.handleWebSocketConnectListener(event);

        // Assert
        verify(presenceService).userConnected(1L);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/presence.1"),
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
        when(event.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

        // Act
        controller.handleWebSocketDisconnectListener(event);

        // Assert
        verify(presenceService).userDisconnected(1L);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/presence.1"),
            argThat((PresenceUpdateResponse response) ->
                response.getUserId().equals(1L) && !response.isOnline()
            )
        );
    }

    @Test
    @DisplayName("Test 6: heartbeat updates presence")
    void testHeartbeatUpdatesPresence() {
        // Act
        controller.handleHeartbeat(principal);

        // Assert
        verify(presenceService).heartbeat(1L);
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
        verify(chatService, never()).sendMessage(anyLong(), anyLong(), anyString(), any());
    }
}
