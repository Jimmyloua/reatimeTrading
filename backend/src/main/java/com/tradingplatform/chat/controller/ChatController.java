package com.tradingplatform.chat.controller;

import com.tradingplatform.chat.dto.*;
import com.tradingplatform.chat.service.ChatMessageCommandService;
import com.tradingplatform.chat.service.ChatService;
import com.tradingplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for chat operations.
 * Implements CHAT-01: Initiate chat with seller.
 * Implements CHAT-03: View chat history.
 */
@Slf4j
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageCommandService chatMessageCommandService;

    /**
     * Creates a new conversation or returns existing one.
     * Implements CHAT-01: User can initiate chat with seller about an item.
     *
     * @param principal the authenticated user
     * @param request the create conversation request
     * @return the conversation response
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        log.debug("Creating conversation for listing {} by user {}",
            request.getListingId(), principal.getId());
        return ResponseEntity.ok(chatService.createConversation(principal.getId(), request));
    }

    /**
     * Gets all conversations for the current user.
     * Implements D-17: Conversations ordered by most recent message.
     *
     * @param principal the authenticated user
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated conversations
     */
    @GetMapping
    public ResponseEntity<Page<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(chatService.getConversations(principal.getId(), pageable));
    }

    /**
     * Gets a single conversation by ID.
     *
     * @param id the conversation ID
     * @param principal the authenticated user
     * @return the conversation response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.getConversationById(id, principal.getId()));
    }

    /**
     * Gets all messages in a conversation.
     * Implements CHAT-03: User can view complete chat history.
     * Also marks messages as read when fetched.
     *
     * @param id the conversation ID
     * @param principal the authenticated user
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated messages
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Mark messages as read when viewing
        chatService.markMessagesAsRead(id, principal.getId());

        return ResponseEntity.ok(chatService.getMessages(id, principal.getId(), pageable));
    }

    /**
     * Sends a message in a conversation.
     * Implements CHAT-04: Message persistence (messages stored in database).
     *
     * @param request the send message request
     * @param principal the authenticated user
     * @return the persisted acknowledgment
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageAck> sendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {
        log.debug("Sending message in conversation {} by user {}", id, principal.getId());

        ChatMessageCommandService.PersistedChatMessage response = chatMessageCommandService.persistMessage(
            new ChatMessageCommandService.SendChatMessageCommand(
                id,
                principal.getId(),
                request.getContent(),
                request.getImageUrl(),
                request.getClientMessageId()
            )
        );
        return ResponseEntity.ok(response.ack());
    }

    /**
     * Marks all messages in a conversation as read.
     * Implements D-06: Read receipts.
     *
     * @param id the conversation ID
     * @param principal the authenticated user
     * @return no content response
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        chatService.markMessagesAsRead(id, principal.getId());
        return ResponseEntity.ok().build();
    }
}
