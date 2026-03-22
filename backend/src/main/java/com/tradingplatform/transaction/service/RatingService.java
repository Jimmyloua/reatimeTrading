package com.tradingplatform.transaction.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.notification.entity.NotificationType;
import com.tradingplatform.notification.service.NotificationService;
import com.tradingplatform.transaction.dto.*;
import com.tradingplatform.transaction.entity.Rating;
import com.tradingplatform.transaction.entity.Transaction;
import com.tradingplatform.transaction.mapper.RatingMapper;
import com.tradingplatform.transaction.repository.RatingRepository;
import com.tradingplatform.transaction.repository.TransactionRepository;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for rating operations.
 * Implements D-29 to D-41: Rating timing, blind rating, aggregation.
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RatingMapper ratingMapper;

    // D-31: 14-day rating window
    private static final int RATING_WINDOW_DAYS = 14;

    /**
     * Submit a rating for a transaction.
     * Per D-29: Only after SETTLED state.
     * Per D-30: Blind rating - hidden until both parties submit.
     */
    @Transactional
    public RatingResponse submitRating(Long transactionId, Long raterId, RatingRequest request) {
        // Get transaction
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        // Validate participant
        if (!transaction.isParticipant(raterId)) {
            throw new ApiException(ErrorCode.NOT_TRANSACTION_PARTICIPANT);
        }

        // Validate status - per D-29
        if (!transaction.getStatus().allowsRating()) {
            throw new ApiException(ErrorCode.TRANSACTION_NOT_ELIGIBLE_FOR_RATING);
        }

        // Check rating window - per D-31
        LocalDateTime settledAt = transaction.getSettledAt();
        if (settledAt == null) {
            throw new ApiException(ErrorCode.TRANSACTION_NOT_ELIGIBLE_FOR_RATING);
        }
        if (settledAt.plusDays(RATING_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.RATING_WINDOW_EXPIRED);
        }

        // Check for existing rating - per D-34 (no edits)
        if (ratingRepository.existsByTransactionIdAndRaterId(transactionId, raterId)) {
            throw new ApiException(ErrorCode.ALREADY_RATED);
        }

        // Determine rated user - per D-37 (bidirectional)
        Long ratedUserId = raterId.equals(transaction.getBuyerId())
            ? transaction.getSellerId()
            : transaction.getBuyerId();

        // Create rating with visibility=false (blind rating per D-30)
        Rating rating = Rating.builder()
            .transactionId(transactionId)
            .raterId(raterId)
            .ratedUserId(ratedUserId)
            .rating(request.getRating())
            .reviewText(request.getReviewText())
            .visible(false)
            .build();

        rating = ratingRepository.save(rating);

        // Check if both parties have rated - reveal if so
        revealRatingsIfComplete(transactionId);

        // Populate rater name for response
        User rater = userRepository.findById(raterId).orElse(null);
        RatingResponse response = ratingMapper.toResponse(rating);
        if (rater != null) {
            response.setRaterName(rater.getDisplayNameOrFallback());
        }

        return response;
    }

    /**
     * Reveal ratings if both parties have submitted.
     * Per D-30: Blind rating reveal.
     */
    @Transactional
    public void revealRatingsIfComplete(Long transactionId) {
        List<Rating> ratings = ratingRepository.findByTransactionId(transactionId);

        // Need exactly 2 ratings (buyer + seller) to reveal
        if (ratings.size() == 2) {
            // Reveal both ratings
            ratings.forEach(r -> r.setVisible(true));
            ratingRepository.saveAll(ratings);

            // Update aggregate ratings for both users
            Long user1 = ratings.get(0).getRatedUserId();
            Long user2 = ratings.get(1).getRatedUserId();
            updateUserAggregateRating(user1);
            updateUserAggregateRating(user2);

            // Notify both parties their reviews are visible
            ratings.forEach(r -> {
                Transaction transaction = transactionRepository.findById(r.getTransactionId()).orElse(null);
                if (transaction != null) {
                    notificationService.createNotification(
                        r.getRaterId(),
                        NotificationType.TRANSACTION_UPDATE,
                        "Review Published",
                        "Your review has been published. View the transaction for details.",
                        r.getTransactionId(),
                        "TRANSACTION"
                    );
                }
            });
        }
    }

    /**
     * Update user's aggregate rating fields.
     * Per D-38, D-39, D-41: Profile rating display.
     */
    @Transactional
    public void updateUserAggregateRating(Long userId) {
        Double avgRating = ratingRepository.calculateAverageRatingForUser(userId);
        Long totalRatings = ratingRepository.countByRatedUserIdAndVisibleTrue(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        user.updateRatingAggregate(
            avgRating != null ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP) : null,
            totalRatings != null ? totalRatings.intValue() : 0
        );

        userRepository.save(user);
    }

    /**
     * Get ratings for a user (visible only).
     * Per D-02: Ratings visible on profile.
     */
    @Transactional(readOnly = true)
    public Page<RatingResponse> getUserRatings(Long userId, Pageable pageable) {
        return ratingRepository.findByRatedUserIdAndVisibleTrueOrderByCreatedAtDesc(userId, pageable)
            .map(rating -> {
                RatingResponse response = ratingMapper.toResponse(rating);
                User rater = userRepository.findById(rating.getRaterId()).orElse(null);
                if (rater != null) {
                    response.setRaterName(rater.getDisplayNameOrFallback());
                }
                return response;
            });
    }

    /**
     * Get recent ratings for user profile (last 5).
     * Per D-40: Last 5 reviews visible on profile.
     */
    @Transactional(readOnly = true)
    public List<RatingResponse> getRecentRatings(Long userId) {
        return ratingRepository.findTop5ByRatedUserIdAndVisibleTrueOrderByCreatedAtDesc(userId)
            .stream()
            .map(rating -> {
                RatingResponse response = ratingMapper.toResponse(rating);
                User rater = userRepository.findById(rating.getRaterId()).orElse(null);
                if (rater != null) {
                    response.setRaterName(rater.getDisplayNameOrFallback());
                }
                return response;
            })
            .toList();
    }

    /**
     * Get rating summary for user profile.
     * Per D-03, D-04: Average and count on profile.
     */
    @Transactional(readOnly = true)
    public UserRatingSummary getRatingSummary(Long userId) {
        Double avgRating = ratingRepository.calculateAverageRatingForUser(userId);
        Long totalRatings = ratingRepository.countByRatedUserIdAndVisibleTrue(userId);

        return UserRatingSummary.builder()
            .userId(userId)
            .averageRating(avgRating != null
                ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP)
                : null)
            .totalRatings(totalRatings != null ? totalRatings.intValue() : 0)
            .hasRatings(totalRatings != null && totalRatings > 0)
            .build();
    }

    /**
     * Check if user can rate a transaction.
     */
    @Transactional(readOnly = true)
    public boolean canRate(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null || !transaction.isParticipant(userId)) {
            return false;
        }
        if (!transaction.getStatus().allowsRating()) {
            return false;
        }
        if (transaction.getSettledAt() == null) {
            return false;
        }
        if (transaction.getSettledAt().plusDays(RATING_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            return false;
        }
        return !ratingRepository.existsByTransactionIdAndRaterId(transactionId, userId);
    }
}