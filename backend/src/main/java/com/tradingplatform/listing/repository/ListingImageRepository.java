package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ListingImage entities.
 */
@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {

    /**
     * Find all images for a listing, ordered by display order.
     */
    List<ListingImage> findByListingIdOrderByDisplayOrderAsc(Long listingId);

    /**
     * Find all images for a listing (unordered).
     */
    List<ListingImage> findByListingId(Long listingId);

    /**
     * Find the primary image for a listing.
     */
    Optional<ListingImage> findByListingIdAndIsPrimaryTrue(Long listingId);

    /**
     * Count images for a listing.
     */
    long countByListingId(Long listingId);

    /**
     * Delete all images for a listing.
     */
    void deleteByListingId(Long listingId);

    /**
     * Reset all images to non-primary for a listing.
     */
    @Modifying
    @Query("UPDATE ListingImage i SET i.isPrimary = false WHERE i.listing.id = :listingId")
    void resetPrimaryForListing(@Param("listingId") Long listingId);
}