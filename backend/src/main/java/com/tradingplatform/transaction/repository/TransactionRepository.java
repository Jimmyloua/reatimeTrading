package com.tradingplatform.transaction.repository;

import com.tradingplatform.transaction.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Transaction entities.
 * Provides query methods with pessimistic locking for financial operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find a transaction by ID with pessimistic write lock.
     * Used for state transitions to prevent concurrent modifications.
     *
     * @param id the transaction ID
     * @return the transaction if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);

    /**
     * Find all transactions where the user is the buyer, ordered by creation date.
     *
     * @param buyerId the buyer ID
     * @param pageable pagination parameters
     * @return paginated transactions
     */
    Page<Transaction> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    /**
     * Find all transactions where the user is the seller, ordered by creation date.
     *
     * @param sellerId the seller ID
     * @param pageable pagination parameters
     * @return paginated transactions
     */
    Page<Transaction> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    /**
     * Find all transactions where the user is a participant (buyer or seller).
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return paginated transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.buyerId = :userId OR t.sellerId = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByParticipantId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Check if a transaction exists with the given idempotency key.
     *
     * @param idempotencyKey the idempotency key
     * @return true if exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find a transaction by idempotency key.
     *
     * @param idempotencyKey the idempotency key
     * @return the transaction if found
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}