package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Listing entities with query support.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    /**
     * Find all listings in a specific category.
     */
    List<Listing> findByCategoryId(Long categoryId);

    /**
     * Find all listings by a specific user.
     */
    List<Listing> findByUserId(Long userId);

    /**
     * Find all listings with a specific status.
     */
    List<Listing> findByStatus(ListingStatus status);

    /**
     * Find all listings by user and status.
     */
    List<Listing> findByUserIdAndStatus(Long userId, ListingStatus status);
}