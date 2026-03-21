package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entities with hierarchy support.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by its unique slug.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Find all children of a specific parent category.
     */
    List<Category> findByParentId(Long parentId);

    /**
     * Find all root categories (categories with no parent).
     */
    List<Category> findByParentIsNull();

    /**
     * Check if a category exists by slug.
     */
    boolean existsBySlug(String slug);
}