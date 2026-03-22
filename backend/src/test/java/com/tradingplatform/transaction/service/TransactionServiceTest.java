package com.tradingplatform.transaction.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.repository.ListingRepository;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.service.NotificationService;
import com.tradingplatform.transaction.entity.Transaction;
import com.tradingplatform.transaction.entity.TransactionStatus;
import com.tradingplatform.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for TransactionService.
 * Implements TRAN-01 to TRAN-03 requirements.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransactionService transactionService;

    private Listing availableListing;
    private Listing reservedListing;
    private Listing soldListing;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        availableListing = Listing.builder()
            .id(100L)
            .title("Test Item")
            .price(new BigDecimal("99.99"))
            .userId(1L)
            .status(ListingStatus.AVAILABLE)
            .build();

        reservedListing = Listing.builder()
            .id(101L)
            .title("Reserved Item")
            .price(new BigDecimal("50.00"))
            .userId(1L)
            .status(ListingStatus.RESERVED)
            .build();

        soldListing = Listing.builder()
            .id(102L)
            .title("Sold Item")
            .price(new BigDecimal("75.00"))
            .userId(1L)
            .status(ListingStatus.SOLD)
            .build();

        transaction = Transaction.builder()
            .id(1L)
            .listingId(100L)
            .buyerId(2L)
            .sellerId(1L)
            .amount(new BigDecimal("99.99"))
            .status(TransactionStatus.CREATED)
            .build();
    }

    @Nested
    @DisplayName("TRAN-01: Create Transaction")
    class CreateTransaction {

        // Test 1: createTransaction creates transaction when listing is AVAILABLE
        @Test
        @DisplayName("should create transaction when listing is available")
        void testCreateTransaction() {
            when(transactionRepository.existsByIdempotencyKey("key1")).thenReturn(false);
            when(listingRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(availableListing));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });
            when(listingRepository.save(any(Listing.class))).thenReturn(availableListing);

            Transaction result = transactionService.createTransaction(
                2L, 100L, null, "key1"
            );

            assertNotNull(result);
            assertEquals(2L, result.getBuyerId());
            assertEquals(1L, result.getSellerId());
            assertEquals(TransactionStatus.CREATED, result.getStatus());
            assertEquals(ListingStatus.RESERVED, availableListing.getStatus());

            verify(notificationService).createNotification(
                eq(1L), eq(NotificationType.TRANSACTION_UPDATE), anyString(), anyString(), eq(1L), eq("TRANSACTION")
            );
        }

        // Test 2: createTransaction throws LISTING_NOT_AVAILABLE when listing is RESERVED or SOLD
        @Test
        @DisplayName("should reject transaction when listing not available - RESERVED")
        void testCreateTransactionListingReserved() {
            when(transactionRepository.existsByIdempotencyKey("key2")).thenReturn(false);
            when(listingRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(reservedListing));

            ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.createTransaction(2L, 101L, null, "key2")
            );

            assertEquals(ErrorCode.LISTING_NOT_AVAILABLE, exception.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject transaction when listing not available - SOLD")
        void testCreateTransactionListingSold() {
            when(transactionRepository.existsByIdempotencyKey("key3")).thenReturn(false);
            when(listingRepository.findByIdAndDeletedFalse(102L)).thenReturn(Optional.of(soldListing));

            ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.createTransaction(2L, 102L, null, "key3")
            );

            assertEquals(ErrorCode.LISTING_NOT_AVAILABLE, exception.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        // Test 3: createTransaction handles idempotency - duplicate request returns same transaction
        @Test
        @DisplayName("should handle idempotency key for duplicate requests")
        void testCreateTransactionIdempotency() {
            when(transactionRepository.existsByIdempotencyKey("key1")).thenReturn(true);
            when(transactionRepository.findByIdempotencyKey("key1")).thenReturn(Optional.of(transaction));

            Transaction result = transactionService.createTransaction(
                2L, 100L, null, "key1"
            );

            assertEquals(transaction, result);
            verify(listingRepository, never()).findByIdAndDeletedFalse(any());
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("TRAN-02: Transaction History")
    class TransactionHistory {

        // Test 4: getBuyerTransactions returns paginated transactions for buyer
        @Test
        @DisplayName("should return paginated purchases for buyer")
        void testGetBuyerTransactions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Transaction> page = new PageImpl<>(List.of(transaction));
            when(transactionRepository.findByBuyerIdOrderByCreatedAtDesc(2L, pageable)).thenReturn(page);

            Page<Transaction> result = transactionService.getBuyerTransactions(2L, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(transaction, result.getContent().get(0));
        }

        // Test 5: getSellerTransactions returns paginated transactions for seller
        @Test
        @DisplayName("should return paginated sales for seller")
        void testGetSellerTransactions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Transaction> page = new PageImpl<>(List.of(transaction));
            when(transactionRepository.findBySellerIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

            Page<Transaction> result = transactionService.getSellerTransactions(1L, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(transaction, result.getContent().get(0));
        }
    }

    @Nested
    @DisplayName("TRAN-03: Status Transitions")
    class StatusTransitions {

        // Test 6: transitionStatus validates state machine transitions
        @Test
        @DisplayName("should transition from CREATED to FUNDED when buyer confirms payment")
        void testStatusTransitionCreatedToFunded() {
            transaction.setStatus(TransactionStatus.CREATED);
            when(transactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            Transaction result = transactionService.transitionStatus(1L, TransactionStatus.FUNDED, 2L, "key6");

            assertEquals(TransactionStatus.FUNDED, result.getStatus());
            assertNotNull(result.getFundedAt());
        }

        // Test 7: transitionStatus throws INVALID_STATUS_TRANSITION for invalid transition
        @Test
        @DisplayName("should reject invalid status transitions")
        void testInvalidStatusTransition() {
            transaction.setStatus(TransactionStatus.CREATED);
            when(transactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));

            ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.transitionStatus(1L, TransactionStatus.DELIVERED, 2L, "key7")
            );

            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        }

        // Test 8: cancelTransaction works before FUNDED state
        @Test
        @DisplayName("should cancel transaction before FUNDED state")
        void testCancelTransaction() {
            transaction.setStatus(TransactionStatus.CREATED);
            when(transactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
            when(listingRepository.findById(100L)).thenReturn(Optional.of(availableListing));
            when(listingRepository.save(any(Listing.class))).thenReturn(availableListing);

            Transaction result = transactionService.cancelTransaction(1L, 2L, "Changed mind", "key8");

            assertEquals(TransactionStatus.CANCELLED, result.getStatus());
            assertEquals("Changed mind", result.getCancellationReason());
            assertNotNull(result.getCancelledAt());
            assertEquals(ListingStatus.AVAILABLE, availableListing.getStatus());
        }

        // Test 9: cancelTransaction throws exception after FUNDED state
        @Test
        @DisplayName("should reject cancel after FUNDED state")
        void testCancelTransactionAfterFunded() {
            transaction.setStatus(TransactionStatus.FUNDED);
            when(transactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));

            ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.cancelTransaction(1L, 2L, "Too late", "key9")
            );

            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Authorization")
    class AuthorizationTests {

        @Test
        @DisplayName("getTransactionById throws TRANSACTION_NOT_FOUND for non-existent transaction")
        void testGetTransactionByIdNotFound() {
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.getTransactionById(999L, 1L)
            );

            assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("getTransactionById throws NOT_TRANSACTION_PARTICIPANT for non-participant")
        void testGetTransactionByIdNotParticipant() {
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

            ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.getTransactionById(1L, 999L)
            );

            assertEquals(ErrorCode.NOT_TRANSACTION_PARTICIPANT, exception.getErrorCode());
        }
    }
}