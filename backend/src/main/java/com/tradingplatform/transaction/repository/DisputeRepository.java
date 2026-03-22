package com.tradingplatform.transaction.repository;

import com.tradingplatform.transaction.entity.Dispute;
import com.tradingplatform.transaction.entity.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Dispute entity.
 */
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    /**
     * Finds a dispute by transaction ID.
     *
     * @param transactionId the transaction ID
     * @return the dispute if found
     */
    Optional<Dispute> findByTransactionId(Long transactionId);

    /**
     * Checks if a dispute exists for a transaction.
     *
     * @param transactionId the transaction ID
     * @return true if a dispute exists
     */
    boolean existsByTransactionId(Long transactionId);

    /**
     * Finds disputes by status.
     *
     * @param statuses the list of statuses to filter by
     * @return list of disputes
     */
    List<Dispute> findByStatusIn(List<DisputeStatus> statuses);

    /**
     * Finds disputes opened by a user.
     *
     * @param openerId the opener's user ID
     * @return list of disputes
     */
    List<Dispute> findByOpenerId(Long openerId);
}