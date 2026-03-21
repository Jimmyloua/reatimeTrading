package com.tradingplatform.listing.service;

import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.repository.CategoryRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for listing operations.
 * Stub for Wave 0 - will be implemented in Plan 02-02.
 */
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    public Optional<Listing> findById(Long id) {
        return listingRepository.findById(id);
    }

    public List<Listing> findBySellerId(Long sellerId) {
        return listingRepository.findByUserId(sellerId);
    }

    @Transactional
    public Listing createListing(Listing listing) {
        // TODO: Implement in Plan 02-02
        return listingRepository.save(listing);
    }

    @Transactional
    public Listing updateListing(Long id, Listing listing, Long userId) {
        // TODO: Implement in Plan 02-02
        return listingRepository.save(listing);
    }

    @Transactional
    public void deleteListing(Long id, Long userId) {
        // TODO: Implement in Plan 02-02
        listingRepository.deleteById(id);
    }

    public boolean isOwner(Long listingId, Long userId) {
        return listingRepository.findById(listingId)
                .map(listing -> listing.getSellerId().equals(userId))
                .orElse(false);
    }
}