package com.tradingplatform.listing.service;

import com.tradingplatform.listing.dto.ListingSearchRequest;
import com.tradingplatform.listing.dto.CategoryResponse;
import com.tradingplatform.listing.dto.ListingResponse;
import com.tradingplatform.listing.entity.Category;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.repository.CategoryRepository;
import com.tradingplatform.listing.repository.ListingImageRepository;
import com.tradingplatform.listing.repository.ListingRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for DISC-01,02 (Listing search operations).
 */
@ExtendWith(MockitoExtension.class)
class ListingSearchServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ListingImageRepository listingImageRepository;

    @InjectMocks
    private ListingService listingService;

    private Pageable pageable;
    private Listing listing1;
    private Listing listing2;
    private Category category;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setSlug("electronics");

        listing1 = new Listing();
        listing1.setId(1L);
        listing1.setTitle("iPhone 15 Pro");
        listing1.setDescription("Brand new iPhone");
        listing1.setPrice(new BigDecimal("999.99"));
        listing1.setCategory(category);

        listing2 = new Listing();
        listing2.setId(2L);
        listing2.setTitle("MacBook Pro");
        listing2.setDescription("Used MacBook");
        listing2.setPrice(new BigDecimal("1499.99"));
        listing2.setCategory(category);
    }

    @Test
    @DisplayName("searchListings with query uses full-text search")
    void testFullTextSearch() {
        // Arrange
        String query = "iPhone";
        ListingSearchRequest request = ListingSearchRequest.builder()
                .query(query)
                .build();

        Page<Listing> expectedPage = new PageImpl<>(List.of(listing1));
        when(listingRepository.searchByFullText(any(String.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(listingImageRepository.findByListingIdAndIsPrimaryTrue(listing1.getId())).thenReturn(Optional.empty());

        // Act
        Page<ListingResponse> result = listingService.searchListings(request, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("iPhone 15 Pro", result.getContent().get(0).getTitle());
        verify(listingRepository).searchByFullText("iPhone", pageable);
        verify(listingRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchListings without query uses specification filters")
    void testSpecificationSearch() {
        // Arrange
        ListingSearchRequest request = ListingSearchRequest.builder().build();

        Page<Listing> expectedPage = new PageImpl<>(List.of(listing1, listing2));
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(listingImageRepository.findByListingIdAndIsPrimaryTrue(listing1.getId())).thenReturn(Optional.empty());
        when(listingImageRepository.findByListingIdAndIsPrimaryTrue(listing2.getId())).thenReturn(Optional.empty());

        // Act
        Page<ListingResponse> result = listingService.searchListings(request, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(listingRepository).findAll(any(Specification.class), eq(pageable));
        verify(listingRepository, never()).searchByFullText(any(), any());
    }

    @Test
    @DisplayName("searchListings with categoryId includes child categories")
    void testCategoryFilter() {
        // Arrange
        Long categoryId = 1L;
        List<Long> categoryIds = Arrays.asList(1L, 2L, 3L);

        ListingSearchRequest request = ListingSearchRequest.builder()
                .categoryId(categoryId)
                .build();

        when(categoryRepository.findAllDescendantIds(categoryId))
                .thenReturn(categoryIds);

        Page<Listing> expectedPage = new PageImpl<>(List.of(listing1));
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(listingImageRepository.findByListingIdAndIsPrimaryTrue(listing1.getId())).thenReturn(Optional.empty());

        // Act
        Page<ListingResponse> result = listingService.searchListings(request, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(categoryRepository).findAllDescendantIds(categoryId);
        verify(listingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("searchListings with multiple filters applies all conditions")
    void testCombinedFilters() {
        // Arrange
        BigDecimal minPrice = new BigDecimal("500.00");
        BigDecimal maxPrice = new BigDecimal("1200.00");
        List<Condition> conditions = Arrays.asList(Condition.NEW, Condition.LIKE_NEW);
        String city = "Boston";

        ListingSearchRequest request = ListingSearchRequest.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .conditions(conditions)
                .city(city)
                .build();

        Page<Listing> expectedPage = new PageImpl<>(List.of(listing1));
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(listingImageRepository.findByListingIdAndIsPrimaryTrue(listing1.getId())).thenReturn(Optional.empty());

        // Act
        Page<ListingResponse> result = listingService.searchListings(request, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(listingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("searchListings sanitizes special characters in query")
    void testQuerySanitization() {
        // Arrange
        String dangerousQuery = "iPhone +samsung -used ~new* \"exact\"";
        String expectedSanitized = "iPhone samsung used new exact";

        ListingSearchRequest request = ListingSearchRequest.builder()
                .query(dangerousQuery)
                .build();

        Page<Listing> expectedPage = new PageImpl<>(Collections.emptyList());
        when(listingRepository.searchByFullText(expectedSanitized, pageable))
                .thenReturn(expectedPage);

        // Act
        listingService.searchListings(request, pageable);

        // Assert
        verify(listingRepository).searchByFullText(expectedSanitized, pageable);
    }

    @Test
    @DisplayName("getCategoryTree returns root categories")
    void testGetCategoryTree() {
        // Arrange
        List<Category> rootCategories = Arrays.asList(category);
        when(categoryRepository.findByParentIsNull()).thenReturn(rootCategories);

        // Act
        List<CategoryResponse> result = listingService.getCategoryTree();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getName());
        verify(categoryRepository).findByParentIsNull();
    }

    @Test
    @DisplayName("getCategoryById returns category when found")
    void testGetCategoryById() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Act
        Optional<CategoryResponse> result = listingService.getCategoryById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Electronics", result.get().getName());
        verify(categoryRepository).findById(1L);
    }

    @Test
    @DisplayName("getCategoryById returns empty when not found")
    void testGetCategoryByIdNotFound() {
        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<CategoryResponse> result = listingService.getCategoryById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(categoryRepository).findById(999L);
    }
}
