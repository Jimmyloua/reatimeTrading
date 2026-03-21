package com.tradingplatform.listing.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.repository.ListingImageRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for listing image operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingImageService {

    private final ListingImageRepository listingImageRepository;
    private final ListingRepository listingRepository;
    private final ListingStorageService listingStorageService;

    @Value("${listing.max-images:10}")
    private int maxImages;

    @Value("${listing.max-size:10485760}")
    private long maxSize;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    /**
     * Uploads images for a listing.
     * Validates ownership, checks image count limit, and sets primary image.
     */
    @Transactional
    public List<ListingImage> uploadImages(Long listingId, List<MultipartFile> files, Integer primaryIndex, Long userId) {
        // Validate listing ownership
        Listing listing = listingRepository.findByIdAndDeletedFalse(listingId)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        if (!listing.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.LISTING_ACCESS_DENIED);
        }

        // Check current image count
        long currentCount = listingImageRepository.countByListingId(listingId);
        if (currentCount + files.size() > maxImages) {
            throw new ApiException(ErrorCode.IMAGE_LIMIT_EXCEEDED,
                    "Maximum " + maxImages + " images allowed per listing. Current: " + currentCount + ", attempting to add: " + files.size());
        }

        // Validate all files first
        for (MultipartFile file : files) {
            validateFile(file);
        }

        // Upload and save images
        List<ListingImage> savedImages = new ArrayList<>();
        int order = (int) currentCount;

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String format = getFormat(file.getContentType());
            String filename = listingStorageService.store(file, listingId, order + i, format);

            boolean isPrimary = (primaryIndex != null && primaryIndex == i) ||
                    (primaryIndex == null && i == 0 && currentCount == 0);

            ListingImage image = ListingImage.builder()
                    .listing(listing)
                    .imagePath(filename)
                    .isPrimary(isPrimary)
                    .displayOrder(order + i)
                    .build();

            savedImages.add(listingImageRepository.save(image));
        }

        log.info("Uploaded {} images for listing {} by user {}", files.size(), listingId, userId);
        return savedImages;
    }

    /**
     * Sets an image as primary for a listing.
     */
    @Transactional
    public void setPrimaryImage(Long listingId, Long imageId, Long userId) {
        // Validate listing ownership
        Listing listing = listingRepository.findByIdAndDeletedFalse(listingId)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        if (!listing.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.LISTING_ACCESS_DENIED);
        }

        // Verify image belongs to listing
        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND, "Image not found"));

        if (!image.getListing().getId().equals(listingId)) {
            throw new ApiException(ErrorCode.LISTING_ACCESS_DENIED, "Image does not belong to this listing");
        }

        // Reset all images to non-primary
        listingImageRepository.resetPrimaryForListing(listingId);

        // Set the selected image as primary
        image.setIsPrimary(true);
        listingImageRepository.save(image);

        log.info("Set image {} as primary for listing {} by user {}", imageId, listingId, userId);
    }

    /**
     * Deletes an image from a listing.
     */
    @Transactional
    public void deleteImage(Long listingId, Long imageId, Long userId) {
        // Validate listing ownership
        Listing listing = listingRepository.findByIdAndDeletedFalse(listingId)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        if (!listing.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.LISTING_ACCESS_DENIED);
        }

        // Verify image belongs to listing
        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND, "Image not found"));

        if (!image.getListing().getId().equals(listingId)) {
            throw new ApiException(ErrorCode.LISTING_ACCESS_DENIED, "Image does not belong to this listing");
        }

        // Delete file and database record
        listingStorageService.delete(image.getImagePath());
        listingImageRepository.delete(image);

        log.info("Deleted image {} from listing {} by user {}", imageId, listingId, userId);
    }

    /**
     * Finds all images for a listing.
     */
    public List<ListingImage> findByListingId(Long listingId) {
        return listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
    }

    /**
     * Checks if more images can be added to a listing.
     */
    public boolean canAddMoreImages(Long listingId) {
        long count = listingImageRepository.countByListingId(listingId);
        return count < maxImages;
    }

    /**
     * Validates an uploaded image file.
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_IMAGE, "File is empty");
        }

        if (file.getSize() > maxSize) {
            throw new ApiException(ErrorCode.INVALID_IMAGE,
                    "File exceeds maximum size of " + (maxSize / 1024 / 1024) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException(ErrorCode.INVALID_IMAGE,
                    "Only JPEG, PNG, and WebP images are allowed");
        }
    }

    /**
     * Gets the image format from content type.
     */
    private String getFormat(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}