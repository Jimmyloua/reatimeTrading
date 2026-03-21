package com.tradingplatform.listing.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.dto.CreateListingRequest;
import com.tradingplatform.listing.dto.ListingDetailResponse;
import com.tradingplatform.listing.dto.ListingResponse;
import com.tradingplatform.listing.dto.UpdateListingRequest;
import com.tradingplatform.listing.entity.Category;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.repository.CategoryRepository;
import com.tradingplatform.listing.repository.ListingImageRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for listing CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new listing.
     */
    @Transactional
    public Listing createListing(CreateListingRequest request, Long userId) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND,
                        "Category not found"));

        Listing listing = Listing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .condition(request.getCondition())
                .status(ListingStatus.AVAILABLE)
                .category(category)
                .userId(userId)
                .city(request.getCity())
                .region(request.getRegion())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deleted(false)
                .build();

        Listing saved = listingRepository.save(listing);
        log.info("Created listing {} for user {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Gets a listing by ID with full details.
     */
    @Transactional(readOnly = true)
    public Listing getListingDetail(Long id) {
        return listingRepository.findWithDetailsByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));
    }

    /**
     * Updates a listing. Only the owner can update.
     */
    @Transactional
    public Listing updateListing(Long id, UpdateListingRequest request, Long userId) {
        Listing listing = getListingAndVerifyOwnership(id, userId);

        if (request.getTitle() != null) {
            listing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            listing.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            listing.setPrice(request.getPrice());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND,
                            "Category not found"));
            listing.setCategory(category);
        }
        if (request.getCondition() != null) {
            listing.setCondition(request.getCondition());
        }
        if (request.getCity() != null) {
            listing.setCity(request.getCity());
        }
        if (request.getRegion() != null) {
            listing.setRegion(request.getRegion());
        }
        if (request.getLatitude() != null) {
            listing.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            listing.setLongitude(request.getLongitude());
        }

        Listing saved = listingRepository.save(listing);
        log.info("Updated listing {} by user {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Soft deletes a listing. Only the owner can delete.
     */
    @Transactional
    public void deleteListing(Long id, Long userId) {
        Listing listing = getListingAndVerifyOwnership(id, userId);
        listing.setDeleted(true);
        listingRepository.save(listing);
        log.info("Deleted listing {} by user {}", id, userId);
    }

    /**
     * Updates the status of a listing. Only the owner can update.
     */
    @Transactional
    public Listing updateStatus(Long id, ListingStatus status, Long userId) {
        Listing listing = getListingAndVerifyOwnership(id, userId);
        listing.setStatus(status);
        Listing saved = listingRepository.save(listing);
        log.info("Updated listing {} status to {} by user {}", id, status, userId);
        return saved;
    }

    /**
     * Gets all listings for a user (paginated).
     */
    @Transactional(readOnly = true)
    public Page<Listing> getUserListings(Long userId, Pageable pageable) {
        return listingRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }

    /**
     * Converts a Listing to ListingResponse DTO.
     */
    public ListingResponse toListingResponse(Listing listing) {
        String primaryImageUrl = null;
        ListingImage primaryImage = listingImageRepository
                .findByListingIdAndIsPrimaryTrue(listing.getId())
                .orElse(null);
        if (primaryImage != null) {
            primaryImageUrl = "/uploads/listings/" + primaryImage.getImagePath();
        }

        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .price(listing.getPrice())
                .condition(listing.getCondition())
                .status(listing.getStatus())
                .city(listing.getCity())
                .region(listing.getRegion())
                .primaryImageUrl(primaryImageUrl)
                .categoryId(listing.getCategory().getId())
                .categoryName(listing.getCategory().getName())
                .createdAt(listing.getCreatedAt())
                .build();
    }

    /**
     * Converts a Listing to ListingDetailResponse DTO.
     */
    public ListingDetailResponse toListingDetailResponse(Listing listing) {
        // Get seller info
        User seller = userRepository.findById(listing.getUserId())
                .orElse(null);

        ListingDetailResponse.SellerInfo sellerInfo = null;
        if (seller != null) {
            String avatarUrl = seller.getAvatarPath() != null
                    ? "/uploads/avatars/" + seller.getAvatarPath()
                    : null;

            // Count seller's listings
            Long listingCount = listingRepository.findByUserId(listing.getUserId())
                    .stream()
                    .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                    .count();

            sellerInfo = ListingDetailResponse.SellerInfo.builder()
                    .id(seller.getId())
                    .displayName(seller.getDisplayNameOrFallback())
                    .avatarUrl(avatarUrl)
                    .memberSince(seller.getCreatedAt())
                    .listingCount(listingCount)
                    .build();
        }

        // Get images
        java.util.List<ListingDetailResponse.ImageInfo> images = listing.getImages().stream()
                .map(img -> ListingDetailResponse.ImageInfo.builder()
                        .id(img.getId())
                        .imageUrl("/uploads/listings/" + img.getImagePath())
                        .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .toList();

        return ListingDetailResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .condition(listing.getCondition())
                .status(listing.getStatus())
                .city(listing.getCity())
                .region(listing.getRegion())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .images(images)
                .categoryId(listing.getCategory().getId())
                .categoryName(listing.getCategory().getName())
                .seller(sellerInfo)
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }

    /**
     * Gets a listing and verifies ownership.
     */
    private Listing getListingAndVerifyOwnership(Long id, Long userId) {
        Listing listing = listingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        if (!listing.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.LISTING_ACCESS_DENIED);
        }

        return listing;
    }
}