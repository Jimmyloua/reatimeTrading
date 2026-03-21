package com.tradingplatform.listing.service;

import com.tradingplatform.listing.dto.ListingSearchRequest;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.specification.ListingSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DISC-03,04,05 (Listing specification/filter operations).
 * Tests verify that the Specification correctly builds for various filter combinations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListingSpecificationTest {

    @Test
    @DisplayName("withFilters returns non-null Specification for empty request")
    void testEmptyRequestReturnsSpecification() {
        // Arrange
        ListingSearchRequest request = ListingSearchRequest.builder().build();

        // Act
        Specification<Listing> spec = ListingSpecification.withFilters(request, null);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("withFilters returns Specification with category IDs")
    void testCategoryFilter() {
        // Arrange
        ListingSearchRequest request = ListingSearchRequest.builder()
                .categoryId(1L)
                .build();
        List<Long> categoryIds = Arrays.asList(1L, 2L, 3L);

        // Act
        Specification<Listing> spec = ListingSpecification.withFilters(request, categoryIds);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("withFilters returns Specification with price range")
    void testPriceRangeFilter() {
        // Arrange
        BigDecimal minPrice = new BigDecimal("100.00");
        BigDecimal maxPrice = new BigDecimal("500.00");

        ListingSearchRequest request = ListingSearchRequest.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();

        // Act
        Specification<Listing> spec = ListingSpecification.withFilters(request, null);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("withFilters returns Specification with conditions")
    void testConditionFilter() {
        // Arrange
        List<Condition> conditions = Arrays.asList(Condition.NEW, Condition.LIKE_NEW);

        ListingSearchRequest request = ListingSearchRequest.builder()
                .conditions(conditions)
                .build();

        // Act
        Specification<Listing> spec = ListingSpecification.withFilters(request, null);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("withFilters returns Specification with location filters")
    void testLocationFilter() {
        // Arrange
        ListingSearchRequest request = ListingSearchRequest.builder()
                .city("New York")
                .region("NY")
                .build();

        // Act
        Specification<Listing> spec = ListingSpecification.withFilters(request, null);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("withFilters returns Specification with all filters combined")
    void testCombinedFilters() {
        // Arrange
        List<Long> categoryIds = Arrays.asList(1L, 2L);
        List<Condition> conditions = Arrays.asList(Condition.NEW);
        BigDecimal minPrice = new BigDecimal("100.00");
        BigDecimal maxPrice = new BigDecimal("500.00");

        ListingSearchRequest request = ListingSearchRequest.builder()
                .categoryId(1L)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .conditions(conditions)
                .city("Boston")
                .region("MA")
                .build();

        // Act
        Specification<Listing> spec = ListingSpecification.withFilters(request, categoryIds);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("isAvailable returns non-null Specification")
    void testIsAvailable() {
        // Act
        Specification<Listing> spec = ListingSpecification.isAvailable();

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("priceBetween returns non-null Specification for min and max")
    void testPriceBetween() {
        // Arrange
        BigDecimal minPrice = new BigDecimal("50.00");
        BigDecimal maxPrice = new BigDecimal("200.00");

        // Act
        Specification<Listing> spec = ListingSpecification.priceBetween(minPrice, maxPrice);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("priceBetween handles null values gracefully")
    void testPriceBetweenNulls() {
        // Act
        Specification<Listing> spec = ListingSpecification.priceBetween(null, null);

        // Assert
        assertNotNull(spec, "Specification should not be null even with null bounds");
    }

    @Test
    @DisplayName("inCategories returns non-null Specification")
    void testInCategories() {
        // Arrange
        List<Long> categoryIds = Arrays.asList(1L, 2L, 3L);

        // Act
        Specification<Listing> spec = ListingSpecification.inCategories(categoryIds);

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("inCategories handles empty list gracefully")
    void testInCategoriesEmpty() {
        // Arrange
        List<Long> emptyCategoryIds = Collections.emptyList();

        // Act
        Specification<Listing> spec = ListingSpecification.inCategories(emptyCategoryIds);

        // Assert
        assertNotNull(spec, "Specification should not be null for empty list");
    }

    @Test
    @DisplayName("inCity returns non-null Specification")
    void testInCity() {
        // Act
        Specification<Listing> spec = ListingSpecification.inCity("New York");

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("inCity handles null gracefully")
    void testInCityNull() {
        // Act
        Specification<Listing> spec = ListingSpecification.inCity(null);

        // Assert
        assertNotNull(spec, "Specification should not be null for null city");
    }

    @Test
    @DisplayName("inRegion returns non-null Specification")
    void testInRegion() {
        // Act
        Specification<Listing> spec = ListingSpecification.inRegion("California");

        // Assert
        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("Specification can be combined with AND")
    void testSpecificationCombination() {
        // Arrange
        Specification<Listing> availableSpec = ListingSpecification.isAvailable();
        Specification<Listing> priceSpec = ListingSpecification.priceBetween(
                new BigDecimal("100.00"), new BigDecimal("500.00"));
        Specification<Listing> citySpec = ListingSpecification.inCity("Boston");

        // Act
        Specification<Listing> combined = availableSpec.and(priceSpec).and(citySpec);

        // Assert
        assertNotNull(combined, "Combined specification should not be null");
    }
}