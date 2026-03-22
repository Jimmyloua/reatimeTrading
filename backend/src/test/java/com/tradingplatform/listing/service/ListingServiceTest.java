package com.tradingplatform.listing.service;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.listing.dto.ListingResponse;
import com.tradingplatform.listing.dto.CreateListingRequest;
import com.tradingplatform.listing.dto.UpdateListingRequest;
import com.tradingplatform.listing.entity.Category;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.repository.CategoryRepository;
import com.tradingplatform.listing.repository.ListingImageRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ListingService.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingImageRepository listingImageRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingService listingService;

    private Category testCategory;
    private Listing testListing;
    private User testUser;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .displayName("Test User")
                .build();

        testListing = Listing.builder()
                .id(1L)
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("100.00"))
                .condition(Condition.NEW)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(1L)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("createListing creates a new listing with valid data")
    void testCreateListing() {
        // Arrange
        CreateListingRequest request = CreateListingRequest.builder()
                .title("New Listing")
                .description("New description")
                .price(new BigDecimal("200.00"))
                .categoryId(1L)
                .condition(Condition.LIKE_NEW)
                .city("New York")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing l = invocation.getArgument(0);
            l.setId(2L);
            return l;
        });

        // Act
        Listing result = listingService.createListing(request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("New Listing", result.getTitle());
        assertEquals(new BigDecimal("200.00"), result.getPrice());
        assertEquals(Condition.LIKE_NEW, result.getCondition());
        assertEquals(ListingStatus.AVAILABLE, result.getStatus());
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    @DisplayName("updateListing modifies an existing listing")
    void testUpdateListing() {
        // Arrange
        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Updated Title")
                .price(new BigDecimal("150.00"))
                .build();

        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        Listing result = listingService.updateListing(1L, request, 1L);

        // Assert
        assertEquals("Updated Title", testListing.getTitle());
        assertEquals(new BigDecimal("150.00"), testListing.getPrice());
        verify(listingRepository).save(testListing);
    }

    @Test
    @DisplayName("deleteListing removes a listing (soft delete)")
    void testDeleteListing() {
        // Arrange
        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        listingService.deleteListing(1L, 1L);

        // Assert
        assertTrue(testListing.getDeleted());
        verify(listingRepository).save(testListing);
    }

    @Test
    @DisplayName("updateListing rejects update from non-owner")
    void testOwnershipValidation() {
        // Arrange
        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Updated Title")
                .build();

        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> listingService.updateListing(1L, request, 2L));

        assertEquals(ErrorCode.LISTING_ACCESS_DENIED, exception.getErrorCode());
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus changes listing status")
    void testUpdateStatus() {
        // Arrange
        when(listingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        Listing result = listingService.updateStatus(1L, ListingStatus.SOLD, 1L);

        // Assert
        assertEquals(ListingStatus.SOLD, testListing.getStatus());
        verify(listingRepository).save(testListing);
    }

    @Test
    @DisplayName("getListingDetail returns listing with details")
    void testGetListingDetail() {
        // Arrange
        when(listingRepository.findWithDetailsByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testListing));

        // Act
        Listing result = listingService.getListingDetail(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Listing", result.getTitle());
    }

    @Test
    @DisplayName("getListingDetail throws exception for non-existent listing")
    void testGetListingDetailNotFound() {
        // Arrange
        when(listingRepository.findWithDetailsByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> listingService.getListingDetail(999L));

        assertEquals(ErrorCode.LISTING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getUserListings returns paginated listings")
    void testGetUserListings() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Listing> page = new PageImpl<>(List.of(testListing));
        when(listingRepository.findByUserIdAndDeletedFalse(1L, pageable)).thenReturn(page);
        when(listingImageRepository.findByListingIdAndIsPrimaryTrue(testListing.getId())).thenReturn(Optional.empty());

        // Act
        Page<ListingResponse> result = listingService.getUserListings(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Listing", result.getContent().get(0).getTitle());
        verify(listingRepository).findByUserIdAndDeletedFalse(1L, pageable);
    }
}
