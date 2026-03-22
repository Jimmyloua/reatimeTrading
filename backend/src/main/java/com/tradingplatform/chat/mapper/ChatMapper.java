package com.tradingplatform.chat.mapper;

import com.tradingplatform.chat.dto.ConversationResponse;
import com.tradingplatform.chat.dto.MessageResponse;
import com.tradingplatform.chat.entity.ChatMessage;
import com.tradingplatform.chat.entity.Conversation;
import org.mapstruct.*;

/**
 * MapStruct mapper for chat entities to DTOs.
 * Handles conversion between Conversation/ChatMessage entities and their response DTOs.
 */
@Mapper(componentModel = "spring")
public interface ChatMapper {

    /**
     * Converts a Conversation entity to ConversationResponse DTO.
     * The currentUserId is used to determine the other participant and unread count.
     *
     * @param conversation the conversation entity
     * @param currentUserId the current user's ID
     * @return the conversation response DTO
     */
    @Mapping(target = "listingTitle", ignore = true)
    @Mapping(target = "otherUserId", expression = "java(conversation.getOtherParticipantId(currentUserId))")
    @Mapping(target = "otherUserName", ignore = true)
    @Mapping(target = "otherUserAvatar", ignore = true)
    @Mapping(target = "otherUserOnline", ignore = true)
    @Mapping(target = "otherUserLastSeen", ignore = true)
    @Mapping(target = "lastMessage", source = "conversation.lastMessagePreview")
    @Mapping(target = "unreadCount", expression = "java(conversation.getUnreadCountForUser(currentUserId))")
    ConversationResponse toConversationResponse(Conversation conversation, Long currentUserId);

    /**
     * Converts a ChatMessage entity to MessageResponse DTO.
     * The currentUserId is used to determine if the message was sent by the current user.
     *
     * @param message the chat message entity
     * @param currentUserId the current user's ID
     * @return the message response DTO
     */
    @Mapping(target = "senderName", ignore = true)
    @Mapping(target = "isOwnMessage", expression = "java(message.getSenderId().equals(currentUserId))")
    MessageResponse toMessageResponse(ChatMessage message, Long currentUserId);
}
