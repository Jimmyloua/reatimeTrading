package com.tradingplatform.chat.repository;

import com.tradingplatform.chat.entity.ChatMessage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends MessageRepository {

    List<ChatMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterMessageId);

    Optional<ChatMessage> findByIdAndConversationId(Long id, Long conversationId);
}
