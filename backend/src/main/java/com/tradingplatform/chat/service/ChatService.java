package com.tradingplatform.chat.service;

import com.tradingplatform.chat.dto.*;
import com.tradingplatform.chat.entity.*;
import com.tradingplatform.chat.mapper.ChatMapper;
import com.tradingplatform.chat.repository.*;
import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.repository.ListingRepository;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for chat operations.
 * Implements persistence-first approach per ROADMAP critical note:
 * Messages must be written to database before any delivery attempt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;
    private final PresenceService presenceService;
    private final ChatMessageCommandService chatMessageCommandService;

    /**
     * Creates a new conversation or returns existing one for the listing/buyer combination.
     * Implements D-02: Each item creates a separate conversation thread.
     *
     * @param buyerId the buyer's user ID
     * @param request the create conversation request
     * @return the conversation response
     */
    @Transactional
    public ConversationResponse createConversation(Long buyerId, CreateConversationRequest request) {
        Listing listing = listingRepository.findByIdAndDeletedFalse(request.getListingId())
            .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        Long sellerId = listing.getSellerId();

        // Prevent users from chatting with themselves
        if (buyerId.equals(sellerId)) {
            throw new ApiException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        // Check for existing conversation (per D-02: separate thread per item)
        Conversation conversation = conversationRepository
            .findByListingIdAndBuyerId(request.getListingId(), buyerId)
            .orElseGet(() -> {
                Conversation newConv = Conversation.builder()
                    .listingId(request.getListingId())
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .build();
                return conversationRepository.save(newConv);
            });

        // Send initial message if provided
        if (request.getInitialMessage() != null && !request.getInitialMessage().isBlank()) {
            chatMessageCommandService.persistMessage(
                new ChatMessageCommandService.SendChatMessageCommand(
                    conversation.getId(),
                    buyerId,
                    request.getInitialMessage(),
                    null,
                    null
                )
            );
        }

        log.info("Created/fetched conversation {} for listing {} buyer {}",
            conversation.getId(), request.getListingId(), buyerId);

        return enrichConversationResponse(conversation, buyerId);
    }

    /**
     * Gets all conversations for a user, ordered by most recent message.
     * Implements D-17: Conversations ordered by most recent message.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return paginated conversation responses
     */
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(Long userId, Pageable pageable) {
        return conversationRepository.findByParticipantId(userId, pageable)
            .map(conv -> enrichConversationResponse(conv, userId));
    }

    /**
     * Gets a single conversation by ID.
     *
     * @param conversationId the conversation ID
     * @param userId the current user ID
     * @return the conversation response
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND));

        // Verify user is a participant
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw new ApiException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
        }

        return enrichConversationResponse(conversation, userId);
    }

    /**
     * Gets all messages in a conversation.
     * Implements CHAT-03: User can view complete chat history.
     *
     * @param conversationId the conversation ID
     * @param userId the current user ID
     * @param pageable pagination parameters
     * @return paginated message responses
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long conversationId, Long userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND));

        // Verify user is a participant
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw new ApiException(ErrorCode.NOT_CONVERSATION_PARTICIPANT);
        }

        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
            .map(msg -> enrichMessageResponse(msg, userId));
    }

    /**
     * Marks all messages in a conversation as read for a user.
     * Implements D-06: Read receipts.
     *
     * @param conversationId the conversation ID
     * @param userId the current user ID
     */
    @Transactional
    public void markMessagesAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApiException(ErrorCode.CONVERSATION_NOT_FOUND));

        // Mark messages as read
        messageRepository.markAsRead(conversationId, userId, MessageStatus.READ);

        // Reset unread count for the user
        if (userId.equals(conversation.getBuyerId())) {
            conversation.setBuyerUnreadCount(0);
        } else {
            conversation.setSellerUnreadCount(0);
        }
        conversationRepository.save(conversation);

        log.debug("Marked messages as read in conversation {} for user {}", conversationId, userId);
    }

    /**
     * Enriches a conversation response with additional data from related entities.
     */
    private ConversationResponse enrichConversationResponse(Conversation conv, Long currentUserId) {
        ConversationResponse response = chatMapper.toConversationResponse(conv, currentUserId);

        // Fetch listing title
        listingRepository.findById(conv.getListingId())
            .ifPresent(listing -> response.setListingTitle(listing.getTitle()));

        // Fetch other user info
        Long otherId = conv.getOtherParticipantId(currentUserId);
        userRepository.findById(otherId).ifPresent(user -> {
            response.setOtherUserName(user.getDisplayNameOrFallback());
            response.setOtherUserAvatar(user.getAvatarPath());
        });
        response.setOtherUserOnline(presenceService.isUserOnline(otherId));
        response.setOtherUserLastSeen(presenceService.getLastSeenText(otherId));

        return response;
    }

    /**
     * Enriches a message response with sender name.
     */
    private MessageResponse enrichMessageResponse(ChatMessage msg, Long currentUserId) {
        MessageResponse response = chatMapper.toMessageResponse(msg, currentUserId);

        userRepository.findById(msg.getSenderId())
            .ifPresent(user -> response.setSenderName(user.getDisplayNameOrFallback()));

        return response;
    }

}
