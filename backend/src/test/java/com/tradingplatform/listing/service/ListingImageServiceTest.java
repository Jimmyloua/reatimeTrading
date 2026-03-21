package com.tradingplatform.listing.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.repository.ListingImageRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ListingImageService.
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

    private Listing testListing;
    private ListingImage testImage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listingImageService, "maxImages", 10);
        ReflectionTestUtils.setField(listingImageService, "maxSize", 10485760L);

        testListing = Listing.builder()
                .id(1L)
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("100.00"))
                .condition(Condition.NEW)
                .status(ListingStatus.AVAILABLE)
                .userId(1L)
                .deleted(false)
                .build();

        testImage = ListingImage.builder()
                .id(1L)
                .listing(testListing)
                .imagePath("listing_1_1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();
    }

    @Test
    @DisplayName("uploadImages stores images for a listing")
    void testUploadImages() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[100]);

        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));
        when(listingImageRepository.countByListingId(1L)).thenReturn(0L);
        when(listingStorageService.store(any(), eq(1L), eq(0), eq("jpg")))
                .thenReturn("listing_1_0.jpg");
        when(listingImageRepository.save(any(ListingImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<ListingImage> result = listingImageService.uploadImages(
                1L, List.of(file), null, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsPrimary()); // First image should be primary
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    @DisplayName("uploadImages rejects more than 10 images")
    void testImageLimit() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[100]);

        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));
        when(listingImageRepository.countByListingId(1L)).thenReturn(9L); // Already 9 images

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> listingImageService.uploadImages(1L, List.of(file, file), null, 1L));

        assertEquals(ErrorCode.IMAGE_LIMIT_EXCEEDED, exception.getErrorCode());
        verify(listingImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("setPrimaryImage marks an image as primary")
    void testSetPrimaryImage() {
        // Arrange
        ListingImage image2 = ListingImage.builder()
                .id(2L)
                .listing(testListing)
                .imagePath("listing_1_2.jpg")
                .isPrimary(false)
                .displayOrder(1)
                .build();

        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));
        when(listingImageRepository.findById(2L)).thenReturn(Optional.of(image2));
        when(listingImageRepository.save(any(ListingImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        listingImageService.setPrimaryImage(1L, 2L, 1L);

        // Assert
        verify(listingImageRepository).resetPrimaryForListing(1L);
        assertTrue(image2.getIsPrimary());
        verify(listingImageRepository).save(image2);
    }

    @Test
    @DisplayName("uploadImages rejects upload from non-owner")
    void testUploadImagesNonOwner() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[100]);

        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> listingImageService.uploadImages(1L, List.of(file), null, 2L));

        assertEquals(ErrorCode.LISTING_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("deleteImage removes image from listing")
    void testDeleteImage() {
        // Arrange
        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));
        when(listingImageRepository.findById(1L)).thenReturn(Optional.of(testImage));

        // Act
        listingImageService.deleteImage(1L, 1L, 1L);

        // Assert
        verify(listingStorageService).delete("listing_1_1.jpg");
        verify(listingImageRepository).delete(testImage);
    }

    @Test
    @DisplayName("validateFile rejects empty file")
    void testValidateFileEmpty() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image", "", "image/jpeg", new byte[0]);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> listingImageService.validateFile(emptyFile));

        assertEquals(ErrorCode.INVALID_IMAGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("validateFile rejects invalid content type")
    void testValidateFileInvalidType() {
        // Arrange
        MockMultipartFile invalidFile = new MockMultipartFile(
                "image", "test.txt", "text/plain", new byte[100]);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> listingImageService.validateFile(invalidFile));

        assertEquals(ErrorCode.INVALID_IMAGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("canAddMoreImages returns true when under limit")
    void testCanAddMoreImagesUnderLimit() {
        // Arrange
        when(listingImageRepository.countByListingId(1L)).thenReturn(5L);

        // Act
        boolean result = listingImageService.canAddMoreImages(1L);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("canAddMoreImages returns false when at limit")
    void testCanAddMoreImagesAtLimit() {
        // Arrange
        when(listingImageRepository.countByListingId(1L)).thenReturn(10L);

        // Act
        boolean result = listingImageService.canAddMoreImages(1L);

        // Assert
        assertFalse(result);
    }
}