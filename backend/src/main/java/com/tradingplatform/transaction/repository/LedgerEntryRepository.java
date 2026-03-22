package com.tradingplatform.transaction.repository;

import com.tradingplatform.transaction.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for LedgerEntry entities.
 * Supports idempotency checks for financial operations.
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * Check if a ledger entry exists with the given idempotency key.
     *
     * @param idempotencyKey the idempotency key
     * @return true if exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}