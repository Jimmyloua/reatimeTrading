package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ListingImage entities.
 * Stub for Wave 0 - will be implemented in Plan 02-02.
 */
@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {

    List<ListingImage> findByListingIdOrderByDisplayOrderAsc(Long listingId);

    Optional<ListingImage> findByListingIdAndIsPrimaryTrue(Long listingId);

    long countByListingId(Long listingId);

    void deleteByListingId(Long listingId);
}