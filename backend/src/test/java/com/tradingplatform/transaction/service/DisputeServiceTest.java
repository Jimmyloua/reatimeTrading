package com.tradingplatform.transaction.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.service.NotificationService;
import com.tradingplatform.transaction.dto.DisputeRequest;
import com.tradingplatform.transaction.dto.DisputeResolutionRequest;
import com.tradingplatform.transaction.dto.DisputeResponse;
import com.tradingplatform.transaction.entity.Dispute;
import com.tradingplatform.transaction.entity.DisputeStatus;
import com.tradingplatform.transaction.entity.Transaction;
import com.tradingplatform.transaction.entity.TransactionStatus;
import com.tradingplatform.transaction.mapper.DisputeMapper;
import com.tradingplatform.transaction.repository.DisputeRepository;
import com.tradingplatform.transaction.repository.TransactionRepository;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DisputeService.
 * Implements D-15 to D-18: Admin-mediated dispute resolution.
 */
@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DisputeMapper disputeMapper;

    @InjectMocks
    private DisputeService disputeService;

    private Transaction deliveredTransaction;
    private Transaction confirmedTransaction;
    private Transaction fundedTransaction;
    private Dispute openDispute;
    private Dispute resolvedDispute;
    private User buyer;
    private User seller;

    @BeforeEach
    void setUp() {
        buyer = User.builder()
            .id(2L)
            .displayName("Buyer User")
            .build();

        seller = User.builder()
            .id(1L)
            .displayName("Seller User")
            .build();

        deliveredTransaction = Transaction.builder()
            .id(100L)
            .listingId(50L)
            .buyerId(2L)
            .sellerId(1L)
            .amount(new BigDecimal("99.99"))
            .status(TransactionStatus.DELIVERED)
            .build();

        confirmedTransaction = Transaction.builder()
            .id(101L)
            .listingId(51L)
            .buyerId(2L)
            .sellerId(1L)
            .amount(new BigDecimal("149.99"))
            .status(TransactionStatus.CONFIRMED)
            .build();

        fundedTransaction = Transaction.builder()
            .id(102L)
            .listingId(52L)
            .buyerId(2L)
            .sellerId(1L)
            .amount(new BigDecimal("199.99"))
            .status(TransactionStatus.FUNDED)
            .build();

        openDispute = Dispute.builder()
            .id(1L)
            .transactionId(100L)
            .openerId(2L)
            .reason("Item not as described")
            .description("The item does not match the listing description.")
            .status(DisputeStatus.OPEN)
            .build();

        resolvedDispute = Dispute.builder()
            .id(2L)
            .transactionId(101L)
            .openerId(2L)
            .reason("Item not received")
            .description("Item never arrived.")
            .status(DisputeStatus.RESOLVED_BUYER)
            .resolution("Full refund issued to buyer.")
            .resolvedBy(999L)
            .build();
    }

    @Nested
    @DisplayName("openDispute")
    class OpenDispute {

        // Test 1: openDispute creates dispute when transaction is DELIVERED
        @Test
        @DisplayName("should create dispute when transaction is DELIVERED")
        void testOpenDisputeCreatesDisputeWhenDelivered() {
            DisputeRequest request = new DisputeRequest();
            request.setReason("Item not as described");
            request.setDescription("The item does not match the listing description.");

            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(disputeRepository.existsByTransactionId(100L)).thenReturn(false);
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });
            when(transactionRepository.save(any(Transaction.class))).thenReturn(deliveredTransaction);
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                return DisputeResponse.builder()
                    .id(d.getId())
                    .transactionId(d.getTransactionId())
                    .openerId(d.getOpenerId())
                    .reason(d.getReason())
                    .description(d.getDescription())
                    .status(d.getStatus())
                    .build();
            });

            DisputeResponse result = disputeService.openDispute(100L, 2L, request);

            assertNotNull(result);
            assertEquals(100L, result.getTransactionId());
            assertEquals(2L, result.getOpenerId());
            assertEquals(DisputeStatus.OPEN, result.getStatus());

            verify(notificationService, times(2)).createNotification(
                anyLong(), eq(NotificationType.TRANSACTION_UPDATE), anyString(), anyString(), eq(100L), eq("TRANSACTION")
            );
        }

        // Test 2: openDispute throws exception when transaction is not DELIVERED or CONFIRMED
        @Test
        @DisplayName("should throw exception when transaction is not DELIVERED or CONFIRMED")
        void testOpenDisputeRejectsInvalidStatus() {
            DisputeRequest request = new DisputeRequest();
            request.setReason("Item not as described");
            request.setDescription("Test description.");

            when(transactionRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(fundedTransaction));

            ApiException exception = assertThrows(ApiException.class, () ->
                disputeService.openDispute(102L, 2L, request)
            );

            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
            verify(disputeRepository, never()).save(any());
        }

        // Test 3: openDispute sets transaction status to DISPUTED
        @Test
        @DisplayName("should set transaction status to DISPUTED")
        void testOpenDisputeSetsTransactionStatusToDisputed() {
            DisputeRequest request = new DisputeRequest();
            request.setReason("Item not as described");
            request.setDescription("Test description.");

            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(disputeRepository.existsByTransactionId(100L)).thenReturn(false);
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });
            when(transactionRepository.save(any(Transaction.class))).thenReturn(deliveredTransaction);
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenReturn(
                DisputeResponse.builder().id(1L).transactionId(100L).openerId(2L).build()
            );

            disputeService.openDispute(100L, 2L, request);

            assertEquals(TransactionStatus.DISPUTED, deliveredTransaction.getStatus());
        }

        // Test 4: openDispute notifies both parties
        @Test
        @DisplayName("should notify both parties")
        void testOpenDisputeNotifiesBothParties() {
            DisputeRequest request = new DisputeRequest();
            request.setReason("Item not as described");
            request.setDescription("Test description.");

            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(disputeRepository.existsByTransactionId(100L)).thenReturn(false);
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });
            when(transactionRepository.save(any(Transaction.class))).thenReturn(deliveredTransaction);
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenReturn(
                DisputeResponse.builder().id(1L).transactionId(100L).openerId(2L).build()
            );

            disputeService.openDispute(100L, 2L, request);

            // Verify notification to seller (other party)
            verify(notificationService).createNotification(
                eq(1L), eq(NotificationType.TRANSACTION_UPDATE), eq("Dispute Opened"), anyString(), eq(100L), eq("TRANSACTION")
            );
            // Verify notification to opener (buyer)
            verify(notificationService).createNotification(
                eq(2L), eq(NotificationType.TRANSACTION_UPDATE), eq("Dispute Submitted"), anyString(), eq(100L), eq("TRANSACTION")
            );
        }

        // Test: openDispute throws exception when dispute already exists
        @Test
        @DisplayName("should throw exception when dispute already exists")
        void testOpenDisputeRejectsDuplicate() {
            DisputeRequest request = new DisputeRequest();
            request.setReason("Item not as described");
            request.setDescription("Test description.");

            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(disputeRepository.existsByTransactionId(100L)).thenReturn(true);

            ApiException exception = assertThrows(ApiException.class, () ->
                disputeService.openDispute(100L, 2L, request)
            );

            assertEquals(ErrorCode.DISPUTE_ALREADY_EXISTS, exception.getErrorCode());
        }

        // Test: openDispute throws exception when user is not participant
        @Test
        @DisplayName("should throw exception when user is not participant")
        void testOpenDisputeRejectsNonParticipant() {
            DisputeRequest request = new DisputeRequest();
            request.setReason("Item not as described");
            request.setDescription("Test description.");

            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));

            ApiException exception = assertThrows(ApiException.class, () ->
                disputeService.openDispute(100L, 999L, request)  // 999 is not a participant
            );

            assertEquals(ErrorCode.NOT_TRANSACTION_PARTICIPANT, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("resolveDispute")
    class ResolveDispute {

        // Test 5: resolveDispute updates dispute status and resolution
        @Test
        @DisplayName("should update dispute status and resolution")
        void testResolveDisputeUpdatesDispute() {
            DisputeResolutionRequest request = new DisputeResolutionRequest();
            request.setStatus(DisputeStatus.RESOLVED_BUYER);
            request.setResolution("Full refund issued to buyer due to item mismatch.");
            request.setAdminId(999L);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(deliveredTransaction);
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenReturn(
                DisputeResponse.builder().id(1L).transactionId(100L).openerId(2L).build()
            );

            DisputeResponse result = disputeService.resolveDispute(1L, request);

            assertEquals(DisputeStatus.RESOLVED_BUYER, openDispute.getStatus());
            assertEquals("Full refund issued to buyer due to item mismatch.", openDispute.getResolution());
            assertEquals(999L, openDispute.getResolvedBy());
            assertNotNull(openDispute.getResolvedAt());
        }

        // Test 6: resolveDispute notifies both parties of outcome
        @Test
        @DisplayName("should notify both parties of outcome")
        void testResolveDisputeNotifiesBothParties() {
            DisputeResolutionRequest request = new DisputeResolutionRequest();
            request.setStatus(DisputeStatus.RESOLVED_BUYER);
            request.setResolution("Full refund issued.");
            request.setAdminId(999L);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(deliveredTransaction);
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenReturn(
                DisputeResponse.builder().id(1L).transactionId(100L).openerId(2L).build()
            );

            disputeService.resolveDispute(1L, request);

            // Verify notification to buyer
            verify(notificationService).createNotification(
                eq(2L), eq(NotificationType.TRANSACTION_UPDATE), eq("Dispute Resolved"), anyString(), eq(100L), eq("TRANSACTION")
            );
            // Verify notification to seller
            verify(notificationService).createNotification(
                eq(1L), eq(NotificationType.TRANSACTION_UPDATE), eq("Dispute Resolved"), anyString(), eq(100L), eq("TRANSACTION")
            );
        }

        // Test: resolveDispute throws exception when dispute already resolved
        @Test
        @DisplayName("should throw exception when dispute already resolved")
        void testResolveDisputeRejectsAlreadyResolved() {
            DisputeResolutionRequest request = new DisputeResolutionRequest();
            request.setStatus(DisputeStatus.RESOLVED_SELLER);
            request.setResolution("Funds released to seller.");

            when(disputeRepository.findById(2L)).thenReturn(Optional.of(resolvedDispute));

            ApiException exception = assertThrows(ApiException.class, () ->
                disputeService.resolveDispute(2L, request)
            );

            assertEquals(ErrorCode.DISPUTE_ALREADY_RESOLVED, exception.getErrorCode());
        }

        // Test: resolveDispute sets transaction status to REFUNDED for RESOLVED_BUYER
        @Test
        @DisplayName("should set transaction status to REFUNDED for RESOLVED_BUYER")
        void testResolveDisputeSetsRefundedForBuyer() {
            DisputeResolutionRequest request = new DisputeResolutionRequest();
            request.setStatus(DisputeStatus.RESOLVED_BUYER);
            request.setResolution("Full refund issued.");
            request.setAdminId(999L);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(deliveredTransaction);
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenReturn(
                DisputeResponse.builder().id(1L).transactionId(100L).openerId(2L).build()
            );

            disputeService.resolveDispute(1L, request);

            assertEquals(TransactionStatus.REFUNDED, deliveredTransaction.getStatus());
        }

        // Test: resolveDispute sets transaction status to SETTLED for RESOLVED_SELLER
        @Test
        @DisplayName("should set transaction status to SETTLED for RESOLVED_SELLER")
        void testResolveDisputeSetsSettledForSeller() {
            DisputeResolutionRequest request = new DisputeResolutionRequest();
            request.setStatus(DisputeStatus.RESOLVED_SELLER);
            request.setResolution("Funds released to seller.");
            request.setAdminId(999L);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(deliveredTransaction);
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenReturn(
                DisputeResponse.builder().id(1L).transactionId(100L).openerId(2L).build()
            );

            disputeService.resolveDispute(1L, request);

            assertEquals(TransactionStatus.SETTLED, deliveredTransaction.getStatus());
        }
    }

    @Nested
    @DisplayName("getDisputeByTransaction")
    class GetDisputeByTransaction {

        @Test
        @DisplayName("should return dispute for transaction participant")
        void testGetDisputeByTransaction() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(disputeRepository.findByTransactionId(100L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
            when(disputeMapper.toResponse(any(Dispute.class))).thenReturn(
                DisputeResponse.builder()
                    .id(1L)
                    .transactionId(100L)
                    .openerId(2L)
                    .reason("Item not as described")
                    .status(DisputeStatus.OPEN)
                    .build()
            );

            DisputeResponse result = disputeService.getDisputeByTransaction(100L, 2L);

            assertNotNull(result);
            assertEquals(100L, result.getTransactionId());
        }

        @Test
        @DisplayName("should throw exception for non-participant")
        void testGetDisputeByTransactionNotParticipant() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(deliveredTransaction));

            ApiException exception = assertThrows(ApiException.class, () ->
                disputeService.getDisputeByTransaction(100L, 999L)
            );

            assertEquals(ErrorCode.NOT_TRANSACTION_PARTICIPANT, exception.getErrorCode());
        }

        @Test
        @DisplayName("should throw exception when dispute not found")
        void testGetDisputeByTransactionNotFound() {
            deliveredTransaction.setStatus(TransactionStatus.DELIVERED);
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(deliveredTransaction));
            when(disputeRepository.findByTransactionId(100L)).thenReturn(Optional.empty());

            ApiException exception = assertThrows(ApiException.class, () ->
                disputeService.getDisputeByTransaction(100L, 2L)
            );

            assertEquals(ErrorCode.DISPUTE_NOT_FOUND, exception.getErrorCode());
        }
    }
}