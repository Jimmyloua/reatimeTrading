package com.tradingplatform.content.repository;

import com.tradingplatform.content.entity.CuratedCollection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuratedCollectionRepository extends JpaRepository<CuratedCollection, Long> {

    @EntityGraph(attributePaths = {"items", "items.listing", "items.listing.category"})
    List<CuratedCollection> findAllByActiveTrueOrderByDisplayOrderAsc();

    @EntityGraph(attributePaths = {"items", "items.listing", "items.listing.category"})
    Optional<CuratedCollection> findBySlugAndActiveTrue(String slug);
}
