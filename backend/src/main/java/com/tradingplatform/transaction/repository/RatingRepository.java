package com.tradingplatform.transaction.repository;

import com.tradingplatform.transaction.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    /**
     * Find all ratings for a transaction (for blind reveal check).
     */
    List<Rating> findByTransactionId(Long transactionId);

    /**
     * Find visible ratings for a user (for profile display).
     * Per D-02: Ratings visible on profile.
     */
    Page<Rating> findByRatedUserIdAndVisibleTrueOrderByCreatedAtDesc(
        Long ratedUserId, Pageable pageable);

    /**
     * Get recent visible ratings for a user (last 5 for profile).
     * Per D-40: Last 5 reviews visible on profile.
     */
    List<Rating> findTop5ByRatedUserIdAndVisibleTrueOrderByCreatedAtDesc(Long ratedUserId);

    /**
     * Count visible ratings for a user.
     * Per D-04: Total number of ratings received.
     */
    Long countByRatedUserIdAndVisibleTrue(Long ratedUserId);

    /**
     * Calculate average rating for a user.
     * Per D-03: Average rating score on profile.
     */
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.ratedUserId = :userId AND r.visible = true")
    Double calculateAverageRatingForUser(@Param("userId") Long userId);

    /**
     * Check if user has already rated a transaction.
     */
    boolean existsByTransactionIdAndRaterId(Long transactionId, Long raterId);

    /**
     * Get rating by transaction and rater.
     */
    Optional<Rating> findByTransactionIdAndRaterId(Long transactionId, Long raterId);

    /**
     * Count total ratings for a transaction (for blind reveal).
     */
    Long countByTransactionId(Long transactionId);
}