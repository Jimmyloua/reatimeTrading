package com.tradingplatform.listing.service;

import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.repository.CategoryRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for listing search operations.
 * Stub for Wave 0 - will be implemented in Plan 02-03.
 */
@Service
@RequiredArgsConstructor
public class ListingSearchService {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    public Page<Listing> search(String query, Pageable pageable) {
        // TODO: Implement full-text search in Plan 02-03
        return listingRepository.findAll(pageable);
    }

    public Page<Listing> searchByCategory(Long categoryId, Pageable pageable) {
        // TODO: Implement category hierarchy search in Plan 02-03
        return listingRepository.findAll(pageable);
    }

    public Page<Listing> search(Specification<Listing> specification, Pageable pageable) {
        return listingRepository.findAll(specification, pageable);
    }

    public Page<Listing> searchWithFilters(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String condition,
            Double latitude,
            Double longitude,
            Double radiusKm,
            Pageable pageable) {
        // TODO: Implement combined filter search in Plan 02-03
        return listingRepository.findAll(pageable);
    }
}