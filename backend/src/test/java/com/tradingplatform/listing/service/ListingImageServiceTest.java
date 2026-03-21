package com.tradingplatform.listing.service;

import com.tradingplatform.listing.repository.ListingImageRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test stubs for LIST-02 (Listing image operations).
 * These tests will be implemented in Plan 02-02.
 */
@ExtendWith(MockitoExtension.class)
class ListingImageServiceTest {

    @Mock
    private ListingImageRepository listingImageRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingStorageService listingStorageService;

    @InjectMocks
    private ListingImageService listingImageService;

    @Test
    @DisplayName("uploadImages stores images for a listing")
    void testUploadImages() {
        // TODO: Implement in Plan 02-02
    }

    @Test
    @DisplayName("uploadImages rejects more than 10 images")
    void testImageLimit() {
        // TODO: Implement in Plan 02-02
    }

    @Test
    @DisplayName("setPrimaryImage marks an image as primary")
    void testSetPrimaryImage() {
        // TODO: Implement in Plan 02-02
    }
}