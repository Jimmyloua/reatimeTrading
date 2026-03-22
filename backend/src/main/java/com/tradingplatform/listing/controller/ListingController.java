package com.tradingplatform.listing.controller;

import com.tradingplatform.listing.dto.*;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.service.ListingImageService;
import com.tradingplatform.listing.service.ListingService;
import com.tradingplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for listing CRUD operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final ListingImageService listingImageService;

    /**
     * Create a new listing.
     */
    @PostMapping
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Listing listing = listingService.createListing(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.toListingResponse(listing));
    }

    /**
     * Get a listing by ID with full details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailResponse> getListingDetail(@PathVariable Long id) {
        Listing listing = listingService.getListingDetail(id);
        return ResponseEntity.ok(listingService.toListingDetailResponse(listing));
    }

    /**
     * Update a listing. Only the owner can update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Listing listing = listingService.updateListing(id, request, principal.getId());
        return ResponseEntity.ok(listingService.toListingResponse(listing));
    }

    /**
     * Delete a listing (soft delete). Only the owner can delete.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        listingService.deleteListing(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Update listing status. Only the owner can update.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ListingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Listing listing = listingService.updateStatus(id, request.getStatus(), principal.getId());
        return ResponseEntity.ok(listingService.toListingResponse(listing));
    }

    /**
     * Upload images for a listing. Only the owner can upload.
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<List<ImageUploadResponse>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "primaryIndex", defaultValue = "0") int primaryIndex,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ListingImage> images = listingImageService.uploadImages(
                id, files, primaryIndex, principal.getId());
        return ResponseEntity.ok(images.stream()
                .map(img -> ImageUploadResponse.builder()
                        .id(img.getId())
                        .imageUrl("/uploads/listings/" + img.getImagePath())
                        .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                        .build())
                .toList());
    }

    /**
     * Delete an image from a listing. Only the owner can delete.
     */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        listingImageService.deleteImage(id, imageId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Set an image as primary. Only the owner can set.
     */
    @PatchMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        listingImageService.setPrimaryImage(id, imageId, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Search listings with optional filters and pagination.
     * Supports full-text search, category filtering, price range, condition, and location.
     */
    @GetMapping
    public ResponseEntity<Page<ListingResponse>> searchListings(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<Condition> conditions,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        ListingSearchRequest request = ListingSearchRequest.builder()
                .query(query)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .conditions(conditions)
                .city(city)
                .region(region)
                .build();

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<ListingResponse> results = listingService.searchListings(request, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get all root categories (category tree).
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
        return ResponseEntity.ok(listingService.getCategoryTree());
    }

    /**
     * Get a category by ID.
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return listingService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Parse sort parameter from string format "field,direction".
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("createdAt").descending();
        }
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        }
        return Sort.by(sort).descending();
    }

    /**
     * Get all listings for a specific user (paginated).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ListingResponse>> getUserListings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ListingResponse> listings = listingService.getUserListings(userId, pageable);
        return ResponseEntity.ok(listings);
    }
}
