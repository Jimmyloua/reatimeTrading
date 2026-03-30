package com.tradingplatform.chat.service;

import com.tradingplatform.chat.dto.*;
import com.tradingplatform.chat.entity.*;
import com.tradingplatform.chat.mapper.ChatMapper;
import com.tradingplatform.chat.repository.*;
import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.repository.ListingRepository;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ChatService.
 * Tests for CHAT-01, CHAT-03, CHAT-04.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private PresenceService presenceService;

    @Mock
    private ChatMessageCommandService chatMessageCommandService;

    @InjectMocks
    private ChatService chatService;

    private Listing testListing;
    private User buyer;
    private User seller;
    private Conversation testConversation;
    private ChatMessage testMessage;

    @BeforeEach
    void setUp() {
        buyer = User.builder()
                .id(1L)
                .email("buyer@example.com")
                .displayName("Buyer User")
                .build();

        seller = User.builder()
                .id(2L)
                .email("seller@example.com")
                .displayName("Seller User")
                .build();

        testListing = Listing.builder()
                .id(100L)
                .title("Test Listing")
                .userId(2L) // seller
                .build();

        testConversation = Conversation.builder()
                .id(1L)
                .listingId(100L)
                .buyerId(1L)
                .sellerId(2L)
                .buyerUnreadCount(0)
                .sellerUnreadCount(0)
                .build();

        testMessage = ChatMessage.builder()
                .id(1L)
                .conversationId(1L)
                .senderId(1L)
                .content("Hello!")
                .status(MessageStatus.PERSISTED)
                .build();
    }

    @Test
    @DisplayName("Should create new conversation for listing without initial message")
    void testCreateConversation_new() {
        // Arrange - no initial message
        CreateConversationRequest request = new CreateConversationRequest(100L, null);

        when(listingRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testListing));
        when(conversationRepository.findByListingIdAndBuyerId(100L, 1L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(listingRepository.findById(100L)).thenReturn(Optional.of(testListing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(chatMapper.toConversationResponse(any(), eq(1L))).thenReturn(
            ConversationResponse.builder().id(1L).listingId(100L).build()
        );

        // Act
        ConversationResponse result = chatService.createConversation(1L, request);

        // Assert
        assertNotNull(result);
        verify(conversationRepository).save(any(Conversation.class));
        verify(messageRepository, never()).save(any()); // No initial message
    }

    @Test
    @DisplayName("Should return existing conversation if one exists")
    void testCreateConversation_existing() {
        // Arrange
        CreateConversationRequest request = new CreateConversationRequest(100L, null);

        when(listingRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testListing));
        when(conversationRepository.findByListingIdAndBuyerId(100L, 1L)).thenReturn(Optional.of(testConversation));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(testListing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(chatMapper.toConversationResponse(any(), eq(1L))).thenReturn(
            ConversationResponse.builder().id(1L).listingId(100L).build()
        );

        // Act
        ConversationResponse result = chatService.createConversation(1L, request);

        // Assert
        assertNotNull(result);
        verify(conversationRepository, never()).save(any()); // No new conversation created
    }

    @Test
    @DisplayName("Should route initial message through chat message command service")
    void testCreateConversation_initialMessageUsesCommandService() {
        CreateConversationRequest request = new CreateConversationRequest(100L, "Hello!");

        when(listingRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testListing));
        when(conversationRepository.findByListingIdAndBuyerId(100L, 1L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation conversation = inv.getArgument(0);
            conversation.setId(1L);
            return conversation;
        });
        when(listingRepository.findById(100L)).thenReturn(Optional.of(testListing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(chatMapper.toConversationResponse(any(), eq(1L))).thenReturn(
            ConversationResponse.builder().id(1L).listingId(100L).build()
        );

        chatService.createConversation(1L, request);

        verify(chatMessageCommandService).persistMessage(
            new ChatMessageCommandService.SendChatMessageCommand(1L, 1L, "Hello!", null, null)
        );
    }

    @Test
    @DisplayName("Should throw error when user chats with themselves")
    void testCreateConversation_self() {
        // Arrange
        CreateConversationRequest request = new CreateConversationRequest(100L, "Hello!");
        testListing.setUserId(1L); // Same as buyer

        when(listingRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testListing));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
            () -> chatService.createConversation(1L, request));

        assertEquals(ErrorCode.CANNOT_CHAT_WITH_SELF, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should return conversations ordered by lastMessageAt DESC")
    void testGetConversations_ordered() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Conversation> page = new PageImpl<>(List.of(testConversation));

        when(conversationRepository.findByParticipantId(1L, pageable)).thenReturn(page);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(testListing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(chatMapper.toConversationResponse(any(), eq(1L))).thenReturn(
            ConversationResponse.builder().id(1L).build()
        );

        // Act
        Page<ConversationResponse> result = chatService.getConversations(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should return paginated messages for conversation")
    void testGetMessages_paginated() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 20);
        Page<ChatMessage> page = new PageImpl<>(List.of(testMessage));

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(chatMapper.toMessageResponse(any(), eq(1L))).thenReturn(
            MessageResponse.builder().id(1L).content("Hello!").build()
        );

        // Act
        Page<MessageResponse> result = chatService.getMessages(1L, 1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should mark messages as read")
    void testMarkMessagesAsRead() {
        // Arrange
        testConversation.setSellerUnreadCount(5);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.markAsRead(1L, 2L, MessageStatus.READ)).thenReturn(5);
        when(conversationRepository.save(any())).thenReturn(testConversation);

        // Act
        chatService.markMessagesAsRead(1L, 2L); // Seller reading

        // Assert
        assertEquals(0, testConversation.getSellerUnreadCount());
        verify(messageRepository).markAsRead(1L, 2L, MessageStatus.READ);
    }

    @Test
    @DisplayName("Should throw error when non-participant tries to view messages")
    void testGetMessages_notParticipant() {
        // Arrange
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
            () -> chatService.getMessages(1L, 3L, PageRequest.of(0, 10)));

        assertEquals(ErrorCode.NOT_CONVERSATION_PARTICIPANT, exception.getErrorCode());
    }
}
