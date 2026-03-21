package com.tradingplatform.chat.repository;

import com.tradingplatform.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Conversation entities.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Find a conversation by listing ID and buyer ID.
     * Used to check if a conversation already exists before creating a new one.
     * Implements D-02: separate thread per item.
     *
     * @param listingId the listing ID
     * @param buyerId the buyer ID
     * @return the conversation if found
     */
    Optional<Conversation> findByListingIdAndBuyerId(Long listingId, Long buyerId);

    /**
     * Find all conversations where the user is a participant (buyer or seller).
     * Ordered by lastMessageAt DESC to show most recent conversations first.
     * Implements D-17: conversations ordered by most recent message.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return paginated conversations
     */
    @Query("SELECT c FROM Conversation c WHERE c.buyerId = :userId OR c.sellerId = :userId ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findByParticipantId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find all active conversations (with messages) for a user.
     * Used for quick access to conversations with recent activity.
     *
     * @param userId the user ID
     * @return list of active conversations
     */
    @Query("SELECT c FROM Conversation c WHERE (c.buyerId = :userId OR c.sellerId = :userId) AND c.lastMessageAt IS NOT NULL ORDER BY c.lastMessageAt DESC")
    List<Conversation> findActiveByParticipantId(@Param("userId") Long userId);
}