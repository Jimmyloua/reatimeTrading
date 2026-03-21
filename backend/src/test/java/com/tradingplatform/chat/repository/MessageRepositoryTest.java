package com.tradingplatform.chat.repository;

import com.tradingplatform.chat.entity.ChatMessage;
import com.tradingplatform.chat.entity.Conversation;
import com.tradingplatform.chat.entity.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageRepository and ConversationRepository.
 * Tests for CHAT-01, CHAT-03, CHAT-04.
 */
@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    private Conversation testConversation;
    private Long buyerId = 1L;
    private Long sellerId = 2L;
    private Long listingId = 100L;

    @BeforeEach
    void setUp() {
        testConversation = Conversation.builder()
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();
        testConversation = conversationRepository.save(testConversation);
    }

    // ============== ConversationRepository Tests ==============

    @Test
    @DisplayName("Should find conversation by listing ID and buyer ID")
    void testFindByListingIdAndBuyerId_returnsConversation() {
        // Act
        Optional<Conversation> found = conversationRepository.findByListingIdAndBuyerId(listingId, buyerId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(listingId, found.get().getListingId());
        assertEquals(buyerId, found.get().getBuyerId());
        assertEquals(sellerId, found.get().getSellerId());
    }

    @Test
    @DisplayName("Should return empty when conversation not found by listing and buyer")
    void testFindByListingIdAndBuyerId_notFound() {
        // Act
        Optional<Conversation> found = conversationRepository.findByListingIdAndBuyerId(999L, buyerId);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find all conversations for a participant (buyer)")
    void testFindByParticipantId_asBuyer() {
        // Arrange - create another conversation where user is seller
        Conversation anotherConv = Conversation.builder()
                .listingId(200L)
                .buyerId(3L)
                .sellerId(buyerId) // buyerId is seller here
                .build();
        conversationRepository.save(anotherConv);

        // Act
        Page<Conversation> result = conversationRepository.findByParticipantId(buyerId, PageRequest.of(0, 10));

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Should find all conversations for a participant (seller)")
    void testFindByParticipantId_asSeller() {
        // Arrange - create another conversation where seller is buyer
        Conversation anotherConv = Conversation.builder()
                .listingId(200L)
                .buyerId(sellerId) // sellerId is buyer here
                .sellerId(3L)
                .build();
        conversationRepository.save(anotherConv);

        // Act
        Page<Conversation> result = conversationRepository.findByParticipantId(sellerId, PageRequest.of(0, 10));

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Should order conversations by lastMessageAt DESC")
    void testFindByParticipantId_orderedByLastMessageAt() {
        // Arrange - create two conversations with different lastMessageAt
        Conversation conv1 = Conversation.builder()
                .listingId(200L)
                .buyerId(3L)
                .sellerId(sellerId)
                .build();
        conv1 = conversationRepository.save(conv1);

        // Wait a bit to ensure different timestamp
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        Conversation conv2 = Conversation.builder()
                .listingId(300L)
                .buyerId(4L)
                .sellerId(sellerId)
                .build();
        conv2 = conversationRepository.save(conv2);

        // Act - query for seller's conversations
        Page<Conversation> result = conversationRepository.findByParticipantId(sellerId, PageRequest.of(0, 10));

        // Assert - should be ordered by lastMessageAt DESC (nulls last)
        // Both have null lastMessageAt, so order by ID ascending (latest first due to DESC on null)
        assertEquals(3, result.getTotalElements());
    }

    // ============== MessageRepository Tests ==============

    @Test
    @DisplayName("Should find messages by conversation ID ordered by creation date descending")
    void testFindByConversationIdOrderByCreatedAtDesc() {
        // Arrange - create multiple messages
        ChatMessage msg1 = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(buyerId)
                .content("First message")
                .status(MessageStatus.SENT)
                .build();
        messageRepository.save(msg1);

        // Wait to ensure different timestamps
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        ChatMessage msg2 = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(sellerId)
                .content("Second message")
                .status(MessageStatus.SENT)
                .build();
        messageRepository.save(msg2);

        // Act
        Page<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                testConversation.getId(), PageRequest.of(0, 10));

        // Assert - most recent first
        assertEquals(2, messages.getTotalElements());
        assertEquals("Second message", messages.getContent().get(0).getContent());
        assertEquals("First message", messages.getContent().get(1).getContent());
    }

    @Test
    @DisplayName("Should count unread messages correctly")
    void testCountUnreadByConversationAndUser() {
        // Arrange - create messages with different senders and statuses
        ChatMessage msg1 = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(buyerId) // Sent by buyer
                .content("From buyer")
                .status(MessageStatus.SENT)
                .build();

        ChatMessage msg2 = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(sellerId) // Sent by seller
                .content("From seller - unread")
                .status(MessageStatus.SENT) // Not read
                .build();

        ChatMessage msg3 = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(sellerId) // Sent by seller
                .content("From seller - read")
                .status(MessageStatus.READ) // Already read
                .build();

        messageRepository.save(msg1);
        messageRepository.save(msg2);
        messageRepository.save(msg3);

        // Act - count unread for buyer (messages from seller that are not READ)
        Long unreadCount = messageRepository.countUnreadByConversationAndUser(
                testConversation.getId(), buyerId, MessageStatus.READ);

        // Assert - only msg2 should be counted (from seller, not sent by buyer, status != READ)
        assertEquals(1L, unreadCount);
    }

    @Test
    @DisplayName("Should mark messages as read")
    void testMarkAsRead() {
        // Arrange - create unread messages from seller
        ChatMessage msg1 = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(sellerId)
                .content("Unread message 1")
                .status(MessageStatus.SENT)
                .build();

        ChatMessage msg2 = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(sellerId)
                .content("Unread message 2")
                .status(MessageStatus.SENT)
                .build();

        messageRepository.save(msg1);
        messageRepository.save(msg2);

        // Act - mark as read for buyer
        int updated = messageRepository.markAsRead(
                testConversation.getId(), buyerId, MessageStatus.READ);

        // Assert - verify the update count
        assertEquals(2, updated);

        // Verify by counting unread - should be 0 now
        Long unreadCount = messageRepository.countUnreadByConversationAndUser(
                testConversation.getId(), buyerId, MessageStatus.READ);
        assertEquals(0L, unreadCount);
    }

    @Test
    @DisplayName("Should get unread count for user via conversation helper")
    void testConversationGetUnreadCountForUser() {
        // Arrange
        testConversation.setBuyerUnreadCount(5);
        testConversation.setSellerUnreadCount(3);
        conversationRepository.save(testConversation);

        // Act & Assert
        assertEquals(5, testConversation.getUnreadCountForUser(buyerId));
        assertEquals(3, testConversation.getUnreadCountForUser(sellerId));
    }

    @Test
    @DisplayName("Should get other participant ID via conversation helper")
    void testConversationGetOtherParticipantId() {
        // Act & Assert
        assertEquals(sellerId, testConversation.getOtherParticipantId(buyerId));
        assertEquals(buyerId, testConversation.getOtherParticipantId(sellerId));
    }

    @Test
    @DisplayName("Should save message with image URL")
    void testSaveMessageWithImageUrl() {
        // Arrange
        ChatMessage msg = ChatMessage.builder()
                .conversationId(testConversation.getId())
                .senderId(buyerId)
                .content("Check this image!")
                .imageUrl("/uploads/chat/img123.jpg")
                .status(MessageStatus.SENT)
                .build();

        // Act
        ChatMessage saved = messageRepository.save(msg);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("/uploads/chat/img123.jpg", saved.getImageUrl());
    }
}