package com.tradingplatform.transaction.controller;

import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.transaction.dto.*;
import com.tradingplatform.transaction.entity.Transaction;
import com.tradingplatform.transaction.mapper.TransactionMapper;
import com.tradingplatform.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for transaction operations.
 * Implements TRAN-01 to TRAN-03 requirements.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    /**
     * Create a new transaction (Request to Buy).
     * Implements TRAN-01: User can mark an item as sold to a specific buyer.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(
            principal.getId(),
            request.getListingId(),
            request.getConversationId(),
            request.getIdempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(transactionMapper.toResponse(transaction));
    }

    /**
     * Get paginated list of purchases for the current user.
     * Implements TRAN-02: User can view transaction history.
     */
    @GetMapping("/purchases")
    public Page<TransactionResponse> getPurchases(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Transaction> transactions = transactionService.getBuyerTransactions(principal.getId(), pageable);
        return transactions.map(transactionMapper::toResponse);
    }

    /**
     * Get paginated list of sales for the current user.
     * Implements TRAN-02: User can view transaction history.
     */
    @GetMapping("/sales")
    public Page<TransactionResponse> getSales(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Transaction> transactions = transactionService.getSellerTransactions(principal.getId(), pageable);
        return transactions.map(transactionMapper::toResponse);
    }

    /**
     * Get detailed information about a specific transaction.
     * Implements TRAN-03: User can see transaction status.
     */
    @GetMapping("/{id}")
    public TransactionDetailResponse getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return transactionService.getTransactionById(id, principal.getId());
    }

    /**
     * Accept a purchase request (seller only).
     */
    @PostMapping("/{id}/accept")
    public TransactionResponse acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionActionRequest request) {
        Transaction transaction = transactionService.acceptRequest(id, principal.getId(), request.getIdempotencyKey());
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Decline a purchase request (seller only).
     */
    @PostMapping("/{id}/decline")
    public TransactionResponse declineRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionActionRequest request) {
        Transaction transaction = transactionService.declineRequest(id, principal.getId(),
            request.getCancellationReason(), request.getIdempotencyKey());
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Buyer confirms payment sent (CREATED -> FUNDED).
     */
    @PostMapping("/{id}/confirm-payment")
    public TransactionResponse confirmPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionActionRequest request) {
        Transaction transaction = transactionService.confirmPayment(id, principal.getId(), request.getIdempotencyKey());
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Seller confirms funds received (FUNDED -> RESERVED).
     */
    @PostMapping("/{id}/confirm-funds")
    public TransactionResponse confirmFunds(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionActionRequest request) {
        Transaction transaction = transactionService.confirmFundsReceived(id, principal.getId(), request.getIdempotencyKey());
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Seller marks item as delivered (RESERVED -> DELIVERED).
     */
    @PostMapping("/{id}/mark-delivered")
    public TransactionResponse markDelivered(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionActionRequest request) {
        Transaction transaction = transactionService.markDelivered(id, principal.getId(), request.getIdempotencyKey());
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Buyer confirms receipt (DELIVERED -> CONFIRMED).
     */
    @PostMapping("/{id}/confirm-receipt")
    public TransactionResponse confirmReceipt(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionActionRequest request) {
        Transaction transaction = transactionService.confirmReceipt(id, principal.getId(), request.getIdempotencyKey());
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Cancel a transaction (before FUNDED state).
     */
    @PostMapping("/{id}/cancel")
    public TransactionResponse cancelTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionActionRequest request) {
        Transaction transaction = transactionService.cancelTransaction(id, principal.getId(),
            request.getCancellationReason(), request.getIdempotencyKey());
        return transactionMapper.toResponse(transaction);
    }
}