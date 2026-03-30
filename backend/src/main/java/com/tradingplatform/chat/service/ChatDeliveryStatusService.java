package com.tradingplatform.chat.service;

import com.tradingplatform.chat.entity.ChatMessage;
import com.tradingplatform.chat.entity.MessageStatus;
import com.tradingplatform.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatDeliveryStatusService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public boolean markDelivered(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalStateException("Chat message not found: " + messageId));

        if (message.getStatus() == MessageStatus.DELIVERED) {
            return false;
        }

        message.setStatus(MessageStatus.DELIVERED);
        chatMessageRepository.save(message);
        return true;
    }
}
