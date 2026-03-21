package com.tradingplatform.listing.service;

import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.repository.ListingImageRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for listing image operations.
 * Stub for Wave 0 - will be implemented in Plan 02-02.
 */
@Service
@RequiredArgsConstructor
public class ListingImageService {

    private final ListingImageRepository listingImageRepository;
    private final ListingRepository listingRepository;
    private final ListingStorageService listingStorageService;

    private static final int MAX_IMAGES = 10;

    public List<ListingImage> findByListingId(Long listingId) {
        return listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
    }

    @Transactional
    public List<ListingImage> uploadImages(Long listingId, List<MultipartFile> files) {
        // TODO: Implement in Plan 02-02
        return List.of();
    }

    @Transactional
    public void setPrimaryImage(Long listingId, Long imageId) {
        // TODO: Implement in Plan 02-02
    }

    @Transactional
    public void deleteImage(Long listingId, Long imageId) {
        // TODO: Implement in Plan 02-02
    }

    public boolean canAddMoreImages(Long listingId) {
        long count = listingImageRepository.countByListingId(listingId);
        return count < MAX_IMAGES;
    }
}