package com.tradingplatform.listing.entity;

import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Listing entity for marketplace items.
 */
@Entity
@Table(name = "listings", indexes = {
    @Index(name = "idx_listings_user", columnList = "user_id"),
    @Index(name = "idx_listings_category", columnList = "category_id"),
    @Index(name = "idx_listings_status", columnList = "status"),
    @Index(name = "idx_listings_price", columnList = "price")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Condition condition = Condition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    private Double latitude;

    private Double longitude;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isPrimary DESC, displayOrder ASC")
    @Builder.Default
    private List<ListingImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Returns the user ID of the seller (alias for userId).
     * Used for ownership checks.
     */
    public Long getSellerId() {
        return this.userId;
    }
}