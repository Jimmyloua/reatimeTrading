package com.tradingplatform.transaction.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.repository.ListingRepository;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.service.NotificationService;
import com.tradingplatform.transaction.dto.TransactionDetailResponse;
import com.tradingplatform.transaction.dto.TransactionResponse;
import com.tradingplatform.transaction.entity.Transaction;
import com.tradingplatform.transaction.entity.TransactionStatus;
import com.tradingplatform.transaction.mapper.TransactionMapper;
import com.tradingplatform.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for transaction operations.
 * Implements TRAN-01 to TRAN-03 requirements.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;
    private final TransactionMapper transactionMapper;

    /**
     * Creates a new transaction for a listing.
     * Implements TRAN-01: User can mark an item as sold to a specific buyer.
     *
     * @param buyerId the buyer's user ID
     * @param listingId the listing ID
     * @param conversationId optional conversation ID for context
     * @param idempotencyKey unique key for duplicate prevention
     * @return the created transaction
     */
    @Transactional
    public Transaction createTransaction(Long buyerId, Long listingId, Long conversationId, String idempotencyKey) {
        // Check idempotency first
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Transaction with idempotency key {} already exists", idempotencyKey);
            return transactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        }

        // Get and validate listing
        Listing listing = listingRepository.findByIdAndDeletedFalse(listingId)
            .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new ApiException(ErrorCode.LISTING_NOT_AVAILABLE);
        }

        // Prevent buyer from buying their own listing
        if (listing.getUserId().equals(buyerId)) {
            throw new ApiException(ErrorCode.LISTING_NOT_AVAILABLE, "Cannot buy your own listing");
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
            .listingId(listingId)
            .buyerId(buyerId)
            .sellerId(listing.getUserId())
            .conversationId(conversationId)
            .amount(listing.getPrice())
            .status(TransactionStatus.CREATED)
            .idempotencyKey(idempotencyKey)
            .build();

        // Reserve listing
        listing.setStatus(ListingStatus.RESERVED);

        // Save transaction and listing
        transaction = transactionRepository.save(transaction);
        listingRepository.save(listing);

        log.info("Created transaction {} for listing {} with buyer {}", transaction.getId(), listingId, buyerId);

        // Notify seller
        notificationService.createNotification(
            listing.getUserId(),
            NotificationType.TRANSACTION_UPDATE,
            "New Purchase Request",
            "Someone wants to buy your item: " + listing.getTitle(),
            transaction.getId(),
            "TRANSACTION"
        );

        return transaction;
    }

    /**
     * Gets paginated transactions for a buyer.
     * Implements TRAN-02: User can view transaction history.
     *
     * @param buyerId the buyer's user ID
     * @param pageable pagination parameters
     * @return paginated transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getBuyerTransactions(Long buyerId, Pageable pageable) {
        return transactionRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
    }

    /**
     * Gets paginated transactions for a seller.
     * Implements TRAN-02: User can view transaction history.
     *
     * @param sellerId the seller's user ID
     * @param pageable pagination parameters
     * @return paginated transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getSellerTransactions(Long sellerId, Pageable pageable) {
        return transactionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
    }

    /**
     * Gets a transaction by ID with authorization check.
     * Implements TRAN-03: User can see transaction status.
     *
     * @param transactionId the transaction ID
     * @param userId the current user's ID
     * @return the transaction detail response
     */
    @Transactional(readOnly = true)
    public TransactionDetailResponse getTransactionById(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.isParticipant(userId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT);
        }

        TransactionDetailResponse response = transactionMapper.toDetailResponse(transaction);
        response.setUserRole(transaction.getRoleForUser(userId));

        // Set available actions based on status and user role
        setAvailableActions(response, transaction, userId);

        return response;
    }

    /**
     * Transitions the transaction to a new status.
     * Implements TRAN-03: Transaction status transitions.
     *
     * @param transactionId the transaction ID
     * @param newStatus the new status
     * @param actorId the user performing the action
     * @param idempotencyKey idempotency key for the action
     * @return the updated transaction
     */
    @Transactional
    public Transaction transitionStatus(Long transactionId, TransactionStatus newStatus, Long actorId, String idempotencyKey) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        // Validate participant
        if (!transaction.isParticipant(actorId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT);
        }

        // Validate state transition
        if (!transaction.getStatus().canTransitionTo(newStatus)) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Validate actor can perform this transition
        validateActorForTransition(transaction, newStatus, actorId);

        // Update status and timestamp
        transaction.setStatus(newStatus);
        setTimestampForStatus(transaction, newStatus);

        transaction = transactionRepository.save(transaction);
        log.info("Transitioned transaction {} from {} to {}", transactionId, transaction.getStatus(), newStatus);

        // Send notification
        notifyStatusChange(transaction, newStatus, actorId);

        return transaction;
    }

    /**
     * Cancels a transaction.
     * Implements D-10: Cancelable before FUNDED state.
     *
     * @param transactionId the transaction ID
     * @param userId the user cancelling
     * @param reason the cancellation reason
     * @param idempotencyKey idempotency key
     * @return the cancelled transaction
     */
    @Transactional
    public Transaction cancelTransaction(Long transactionId, Long userId, String reason, String idempotencyKey) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        // Validate participant
        if (!transaction.isParticipant(userId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT);
        }

        // Validate can cancel (D-10: before FUNDED state)
        if (!transaction.getStatus().isCancelable()) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot cancel transaction after payment has been sent");
        }

        // Update transaction
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setCancellationReason(reason);
        transaction.setCancelledAt(LocalDateTime.now());

        // Release listing back to AVAILABLE
        Listing listing = listingRepository.findById(transaction.getListingId()).orElse(null);
        if (listing != null) {
            listing.setStatus(ListingStatus.AVAILABLE);
            listingRepository.save(listing);
        }

        transaction = transactionRepository.save(transaction);
        log.info("Cancelled transaction {} by user {}", transactionId, userId);

        // Notify other party
        Long otherUserId = transaction.getOtherParticipantId(userId);
        notificationService.createNotification(
            otherUserId,
            NotificationType.TRANSACTION_UPDATE,
            "Transaction Cancelled",
            "The transaction has been cancelled: " + (reason != null ? reason : "No reason provided"),
            transactionId,
            "TRANSACTION"
        );

        return transaction;
    }

    /**
     * Buyer confirms payment sent (CREATED -> FUNDED).
     */
    @Transactional
    public Transaction confirmPayment(Long transactionId, Long buyerId, String idempotencyKey) {
        return transitionStatus(transactionId, TransactionStatus.FUNDED, buyerId, idempotencyKey);
    }

    /**
     * Seller confirms funds received (FUNDED -> RESERVED).
     */
    @Transactional
    public Transaction confirmFundsReceived(Long transactionId, Long sellerId, String idempotencyKey) {
        return transitionStatus(transactionId, TransactionStatus.RESERVED, sellerId, idempotencyKey);
    }

    /**
     * Seller marks item delivered (RESERVED -> DELIVERED).
     */
    @Transactional
    public Transaction markDelivered(Long transactionId, Long sellerId, String idempotencyKey) {
        return transitionStatus(transactionId, TransactionStatus.DELIVERED, sellerId, idempotencyKey);
    }

    /**
     * Buyer confirms receipt (DELIVERED -> CONFIRMED).
     */
    @Transactional
    public Transaction confirmReceipt(Long transactionId, Long buyerId, String idempotencyKey) {
        return transitionStatus(transactionId, TransactionStatus.CONFIRMED, buyerId, idempotencyKey);
    }

    /**
     * Seller accepts the purchase request (kept for API consistency, but CREATED is default).
     * This method is a no-op since transactions start in CREATED state.
     */
    @Transactional
    public Transaction acceptRequest(Long transactionId, Long sellerId, String idempotencyKey) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.getSellerId().equals(sellerId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT, "Only seller can accept");
        }

        // Transaction is already in CREATED state, notify buyer
        notificationService.createNotification(
            transaction.getBuyerId(),
            NotificationType.TRANSACTION_UPDATE,
            "Purchase Request Accepted",
            "The seller has accepted your purchase request. Please proceed with payment.",
            transactionId,
            "TRANSACTION"
        );

        return transaction;
    }

    /**
     * Seller declines the purchase request.
     */
    @Transactional
    public Transaction declineRequest(Long transactionId, Long sellerId, String reason, String idempotencyKey) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.getSellerId().equals(sellerId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT, "Only seller can decline");
        }

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Can only decline in CREATED state");
        }

        // Cancel transaction
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setCancellationReason(reason != null ? reason : "Seller declined the request");
        transaction.setCancelledAt(LocalDateTime.now());

        // Release listing
        Listing listing = listingRepository.findById(transaction.getListingId()).orElse(null);
        if (listing != null) {
            listing.setStatus(ListingStatus.AVAILABLE);
            listingRepository.save(listing);
        }

        transaction = transactionRepository.save(transaction);

        // Notify buyer
        notificationService.createNotification(
            transaction.getBuyerId(),
            NotificationType.TRANSACTION_UPDATE,
            "Purchase Request Declined",
            "The seller has declined your purchase request.",
            transactionId,
            "TRANSACTION"
        );

        return transaction;
    }

    // Private helper methods

    private void validateActorForTransition(Transaction transaction, TransactionStatus newStatus, Long actorId) {
        String role = transaction.getRoleForUser(actorId);
        if (role == null) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT);
        }

        // Validate role-specific transitions per D-09
        boolean valid = switch (newStatus) {
            case FUNDED -> "BUYER".equals(role);  // Buyer confirms payment
            case RESERVED -> "SELLER".equals(role);  // Seller confirms funds
            case DELIVERED -> "SELLER".equals(role);  // Seller marks delivered
            case CONFIRMED -> "BUYER".equals(role);  // Buyer confirms receipt
            case CANCELLED -> true;  // Either party can cancel (if allowed)
            default -> true;
        };

        if (!valid) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT,
                "You are not authorized to perform this action");
        }
    }

    private void setTimestampForStatus(Transaction transaction, TransactionStatus status) {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case FUNDED -> transaction.setFundedAt(now);
            case RESERVED -> transaction.setReservedAt(now);
            case DELIVERED -> transaction.setDeliveredAt(now);
            case CONFIRMED -> transaction.setConfirmedAt(now);
            case SETTLED -> transaction.setSettledAt(now);
            case CANCELLED -> transaction.setCancelledAt(now);
            case EXPIRED -> transaction.setExpiredAt(now);
            default -> {}
        }
    }

    private void notifyStatusChange(Transaction transaction, TransactionStatus newStatus, Long actorId) {
        Long otherUserId = transaction.getOtherParticipantId(actorId);
        String message = getStatusChangeMessage(newStatus);

        notificationService.createNotification(
            otherUserId,
            NotificationType.TRANSACTION_UPDATE,
            "Transaction Updated",
            message,
            transaction.getId(),
            "TRANSACTION"
        );
    }

    private String getStatusChangeMessage(TransactionStatus status) {
        return switch (status) {
            case FUNDED -> "Buyer has confirmed payment sent.";
            case RESERVED -> "Seller has confirmed funds received.";
            case DELIVERED -> "Seller has marked the item as delivered.";
            case CONFIRMED -> "Buyer has confirmed receipt of the item.";
            case SETTLED -> "Funds have been released to the seller.";
            case COMPLETED -> "Transaction completed. You can now leave a review.";
            default -> "Transaction status updated to " + status;
        };
    }

    private void setAvailableActions(TransactionDetailResponse response, Transaction transaction, Long userId) {
        String role = transaction.getRoleForUser(userId);
        TransactionStatus status = transaction.getStatus();

        response.setCanCancel(status.isCancelable());
        response.setCanConfirmPayment("BUYER".equals(role) && status == TransactionStatus.CREATED);
        response.setCanConfirmFunds("SELLER".equals(role) && status == TransactionStatus.FUNDED);
        response.setCanMarkDelivered("SELLER".equals(role) && status == TransactionStatus.RESERVED);
        response.setCanConfirmReceipt("BUYER".equals(role) && status == TransactionStatus.DELIVERED);
        response.setCanRate(status.allowsRating());
        response.setCanDispute(status == TransactionStatus.DELIVERED || status == TransactionStatus.CONFIRMED);
    }
}