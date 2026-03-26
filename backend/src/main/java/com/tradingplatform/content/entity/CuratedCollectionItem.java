package com.tradingplatform.content.entity;

import com.tradingplatform.listing.entity.Listing;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "curated_collection_items", indexes = {
        @Index(name = "idx_collection_items_collection_order", columnList = "collection_id,display_order")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuratedCollectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private CuratedCollection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "badge_text", length = 80)
    private String badgeText;
}
