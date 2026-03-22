package com.tradingplatform.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Rating entity for transaction reviews.
 * Implements D-30: Blind rating - hidden until both parties submit.
 * Implements D-35: 1-5 star rating with optional text.
 * Implements D-36: Rating required, text optional.
 */
@Entity
@Table(name = "ratings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ratings_transaction_rater",
        columnNames = {"transaction_id", "rater_id"}
    ),
    indexes = {
        @Index(name = "idx_ratings_transaction", columnList = "transaction_id"),
        @Index(name = "idx_ratings_rated_user", columnList = "rated_user_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "rater_id", nullable = false)
    private Long raterId;

    @Column(name = "rated_user_id", nullable = false)
    private Long ratedUserId;

    /**
     * Rating value 1-5 stars.
     * Per D-35: 1-5 star rating.
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Optional review text, max 500 characters.
     * Per D-36: Review text optional.
     */
    @Column(name = "review_text", length = 500)
    private String reviewText;

    /**
     * Blind rating visibility flag.
     * Per D-30: Hidden until both parties submit or 14-day window closes.
     */
    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean visible = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Validates rating is in valid range (1-5).
     */
    public boolean isValidRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }
}