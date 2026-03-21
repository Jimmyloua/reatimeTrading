package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Listing entities with query support.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    /**
     * Find a listing by ID that is not soft-deleted.
     */
    Optional<Listing> findByIdAndDeletedFalse(Long id);

    /**
     * Find a listing with all details (images, category, user) by ID, not soft-deleted.
     */
    @EntityGraph(attributePaths = {"images", "category"})
    Optional<Listing> findWithDetailsByIdAndDeletedFalse(Long id);

    /**
     * Find all listings by a specific user that are not soft-deleted.
     */
    Page<Listing> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

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

    /**
     * Find all listings in a category that are available and not deleted.
     */
    @Query("SELECT l FROM Listing l WHERE l.category.id = :categoryId AND l.deleted = false AND l.status = 'AVAILABLE'")
    Page<Listing> findByCategoryIdAndDeletedFalse(@Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * Full-text search on listings by title and description.
     * Uses MySQL FULLTEXT index.
     */
    @Query(value = "SELECT * FROM listings l WHERE MATCH(l.title, l.description) AGAINST(:query IN BOOLEAN MODE) AND l.status = 'AVAILABLE' AND l.is_deleted = false ORDER BY l.created_at DESC", nativeQuery = true)
    Page<Listing> searchByFullText(@Param("query") String query, Pageable pageable);
}