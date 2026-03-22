package com.tradingplatform.transaction.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.service.NotificationService;
import com.tradingplatform.transaction.dto.*;
import com.tradingplatform.transaction.entity.Dispute;
import com.tradingplatform.transaction.entity.DisputeStatus;
import com.tradingplatform.transaction.entity.Transaction;
import com.tradingplatform.transaction.entity.TransactionStatus;
import com.tradingplatform.transaction.mapper.DisputeMapper;
import com.tradingplatform.transaction.repository.DisputeRepository;
import com.tradingplatform.transaction.repository.TransactionRepository;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for dispute operations.
 * Per D-15 to D-18: Admin-mediated dispute resolution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DisputeMapper disputeMapper;

    /**
     * Open a dispute for a transaction.
     * Per D-16: Either party can raise dispute after DELIVERED state.
     * Per D-17: Dispute workflow.
     *
     * @param transactionId the transaction ID
     * @param openerId the user opening the dispute
     * @param request the dispute request
     * @return the created dispute response
     */
    @Transactional
    public DisputeResponse openDispute(Long transactionId, Long openerId, DisputeRequest request) {
        // Validate transaction exists and user is participant
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.isParticipant(openerId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT);
        }

        // Per D-16: Only after DELIVERED or CONFIRMED state
        if (transaction.getStatus() != TransactionStatus.DELIVERED
            && transaction.getStatus() != TransactionStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Check for existing dispute
        if (disputeRepository.existsByTransactionId(transactionId)) {
            throw new ApiException(ErrorCode.DISPUTE_ALREADY_EXISTS);
        }

        // Create dispute
        Dispute dispute = Dispute.builder()
            .transactionId(transactionId)
            .openerId(openerId)
            .reason(request.getReason())
            .description(request.getDescription())
            .status(DisputeStatus.OPEN)
            .build();

        dispute = disputeRepository.save(dispute);

        // Update transaction status to DISPUTED
        transaction.setStatus(TransactionStatus.DISPUTED);
        transactionRepository.save(transaction);

        log.info("Dispute {} opened for transaction {} by user {}", dispute.getId(), transactionId, openerId);

        // Notify both parties per D-17
        String openerName = getUserName(openerId);
        Long otherPartyId = transaction.getOtherParticipantId(openerId);

        notificationService.createNotification(
            otherPartyId,
            NotificationType.TRANSACTION_UPDATE,
            "Dispute Opened",
            openerName + " has opened a dispute for your transaction. A moderator will review the case.",
            transactionId,
            "TRANSACTION"
        );

        notificationService.createNotification(
            openerId,
            NotificationType.TRANSACTION_UPDATE,
            "Dispute Submitted",
            "Your dispute has been submitted. We will review your case and respond within 3 business days.",
            transactionId,
            "TRANSACTION"
        );

        DisputeResponse response = disputeMapper.toResponse(dispute);
        response.setOpenerName(openerName);
        return response;
    }

    /**
     * Get dispute by transaction ID.
     *
     * @param transactionId the transaction ID
     * @param userId the requesting user ID
     * @return the dispute response
     */
    @Transactional(readOnly = true)
    public DisputeResponse getDisputeByTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.isParticipant(userId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT);
        }

        Dispute dispute = disputeRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.DISPUTE_NOT_FOUND));

        DisputeResponse response = disputeMapper.toResponse(dispute);
        response.setOpenerName(getUserName(dispute.getOpenerId()));
        return response;
    }

    /**
     * Resolve a dispute (admin action).
     * Per D-17: Admin-mediated resolution.
     *
     * @param disputeId the dispute ID
     * @param request the resolution request
     * @return the updated dispute response
     */
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, DisputeResolutionRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
            .orElseThrow(() -> new ApiException(ErrorCode.DISPUTE_NOT_FOUND));

        if (dispute.getStatus().isResolved()) {
            throw new ApiException(ErrorCode.DISPUTE_ALREADY_RESOLVED);
        }

        // Update dispute
        dispute.setStatus(request.getStatus());
        dispute.setResolution(request.getResolution());
        dispute.setResolvedBy(request.getAdminId());
        dispute.setResolvedAt(LocalDateTime.now());

        dispute = disputeRepository.save(dispute);

        log.info("Dispute {} resolved with status {} by admin {}",
            disputeId, request.getStatus(), request.getAdminId());

        // Update transaction based on resolution
        Transaction transaction = transactionRepository.findByIdForUpdate(dispute.getTransactionId())
            .orElseThrow();

        if (request.getStatus() == DisputeStatus.RESOLVED_BUYER) {
            transaction.setStatus(TransactionStatus.REFUNDED);
        } else if (request.getStatus() == DisputeStatus.RESOLVED_SELLER) {
            transaction.setStatus(TransactionStatus.SETTLED);
        }

        transactionRepository.save(transaction);

        // Notify both parties
        notificationService.createNotification(
            transaction.getBuyerId(),
            NotificationType.TRANSACTION_UPDATE,
            "Dispute Resolved",
            "Your dispute has been resolved. " + (request.getResolution() != null ? request.getResolution() : ""),
            dispute.getTransactionId(),
            "TRANSACTION"
        );

        notificationService.createNotification(
            transaction.getSellerId(),
            NotificationType.TRANSACTION_UPDATE,
            "Dispute Resolved",
            "The dispute has been resolved. " + (request.getResolution() != null ? request.getResolution() : ""),
            dispute.getTransactionId(),
            "TRANSACTION"
        );

        DisputeResponse response = disputeMapper.toResponse(dispute);
        response.setOpenerName(getUserName(dispute.getOpenerId()));
        return response;
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
            .map(User::getDisplayNameOrFallback)
            .orElse("Unknown User");
    }
}