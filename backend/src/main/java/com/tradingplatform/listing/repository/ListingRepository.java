package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Listing entities.
 * Stub for Wave 0 - will be implemented in Plan 02-01 and 02-02.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByCategoryId(Long categoryId);

    List<Listing> findBySellerId(Long sellerId);

    List<Listing> findByStatus(String status);

    // Full-text search will be added in Plan 02-02
    // Location-based queries will be added in Plan 02-03
}