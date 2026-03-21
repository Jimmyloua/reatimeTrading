package com.tradingplatform.chat.repository;

import com.tradingplatform.chat.entity.ChatMessage;
import com.tradingplatform.chat.entity.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ChatMessage entities.
 */
@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all messages in a conversation, ordered by creation date descending.
     *
     * @param conversationId the conversation ID
     * @param pageable pagination parameters
     * @return paginated messages
     */
    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * Find the most recent messages in a conversation (limited to 50).
     *
     * @param conversationId the conversation ID
     * @return list of recent messages
     */
    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /**
     * Count unread messages in a conversation for a specific user.
     * Messages sent by the user are not counted as unread for them.
     *
     * @param conversationId the conversation ID
     * @param userId the user ID (to exclude their own messages)
     * @param status the status to exclude (READ)
     * @return count of unread messages
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.senderId != :userId AND m.status != :status")
    Long countUnreadByConversationAndUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("status") MessageStatus status);

    /**
     * Mark all messages in a conversation as read for a specific user.
     * Only updates messages not sent by the user and not already read.
     *
     * @param conversationId the conversation ID
     * @param userId the user ID (to exclude their own messages)
     * @param status the status to set (READ)
     * @return number of messages updated
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.status = :status WHERE m.conversationId = :conversationId AND m.senderId != :userId AND m.status != :status")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("status") MessageStatus status);
}