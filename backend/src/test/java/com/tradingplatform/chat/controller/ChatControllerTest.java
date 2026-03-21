package com.tradingplatform.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.chat.dto.*;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.service.ChatService;
import com.tradingplatform.security.JwtTokenProvider;
import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ChatController.
 * Tests for CHAT-01 and CHAT-03.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private UserPrincipal testPrincipal;
    private UsernamePasswordAuthenticationToken testAuth;

    @BeforeEach
    void setUp() {
        testPrincipal = new UserPrincipal(1L, "test@example.com", "password",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        testAuth = new UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.getAuthorities());
    }

    @Test
    @DisplayName("Should create conversation when user initiates chat")
    void testCreateConversation() throws Exception {
        // Arrange
        CreateConversationRequest request = new CreateConversationRequest(100L, "Hello!");
        ConversationResponse response = ConversationResponse.builder()
            .id(1L)
            .listingId(100L)
            .listingTitle("Test Listing")
            .otherUserId(2L)
            .otherUserName("Seller")
            .createdAt(LocalDateTime.now())
            .build();

        when(chatService.createConversation(eq(1L), any(CreateConversationRequest.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/conversations")
                .with(authentication(testAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.listingId").value(100))
            .andExpect(jsonPath("$.listingTitle").value("Test Listing"));
    }

    @Test
    @DisplayName("Should return conversation list for user")
    void testGetConversations() throws Exception {
        // Arrange
        ConversationResponse conv = ConversationResponse.builder()
            .id(1L)
            .listingId(100L)
            .listingTitle("Test Listing")
            .otherUserId(2L)
            .otherUserName("Seller")
            .lastMessage("Hello!")
            .unreadCount(2)
            .build();

        Page<ConversationResponse> page = new PageImpl<>(List.of(conv));
        when(chatService.getConversations(eq(1L), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/conversations")
                .with(authentication(testAuth))
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].unreadCount").value(2));
    }

    @Test
    @DisplayName("Should return messages for conversation")
    void testGetMessages() throws Exception {
        // Arrange
        MessageResponse msg = MessageResponse.builder()
            .id(1L)
            .conversationId(1L)
            .senderId(1L)
            .senderName("Buyer")
            .content("Hello!")
            .status(MessageStatus.SENT)
            .isOwnMessage(true)
            .createdAt(LocalDateTime.now())
            .build();

        Page<MessageResponse> page = new PageImpl<>(List.of(msg));
        when(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class))).thenReturn(page);
        doNothing().when(chatService).markMessagesAsRead(1L, 1L);

        // Act & Assert
        mockMvc.perform(get("/api/conversations/1/messages")
                .with(authentication(testAuth))
                .param("page", "0")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].content").value("Hello!"));

        // Verify messages are marked as read
        verify(chatService).markMessagesAsRead(1L, 1L);
    }

    @Test
    @DisplayName("Should send message in conversation")
    void testSendMessage() throws Exception {
        // Arrange
        SendMessageRequest request = new SendMessageRequest(1L, "Hello!", null);
        MessageResponse response = MessageResponse.builder()
            .id(1L)
            .conversationId(1L)
            .senderId(1L)
            .senderName("Buyer")
            .content("Hello!")
            .status(MessageStatus.SENT)
            .isOwnMessage(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(chatService.sendMessage(1L, 1L, "Hello!", null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/conversations/1/messages")
                .with(authentication(testAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.content").value("Hello!"));
    }

    @Test
    @DisplayName("Should mark messages as read")
    void testMarkAsRead() throws Exception {
        // Arrange
        doNothing().when(chatService).markMessagesAsRead(1L, 1L);

        // Act & Assert
        mockMvc.perform(post("/api/conversations/1/read")
                .with(authentication(testAuth)))
            .andExpect(status().isOk());

        verify(chatService).markMessagesAsRead(1L, 1L);
    }

    @Test
    @DisplayName("Should return single conversation by ID")
    void testGetConversation() throws Exception {
        // Arrange
        ConversationResponse response = ConversationResponse.builder()
            .id(1L)
            .listingId(100L)
            .listingTitle("Test Listing")
            .otherUserId(2L)
            .otherUserName("Seller")
            .build();

        when(chatService.getConversationById(1L, 1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/conversations/1")
                .with(authentication(testAuth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.listingId").value(100));
    }

    @Test
    @DisplayName("Should reject unauthenticated requests")
    void testUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/conversations"))
            .andExpect(status().isUnauthorized());
    }
}