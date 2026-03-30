package com.tradingplatform.chat.repository;

import com.tradingplatform.chat.entity.ChatMessageOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageOutboxRepository extends JpaRepository<ChatMessageOutbox, Long> {

    List<ChatMessageOutbox> findTop100ByStatusOrderByCreatedAtAsc(String status);
}
