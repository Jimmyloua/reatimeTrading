package com.tradingplatform.listing.dto;

import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for listing DTOs validation.
 */
class ListingDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CreateListingRequest validates title min 3 max 200 chars")
    void createListingRequest_validatesTitleSize() {
        // Test title too short
        CreateListingRequest shortTitle = CreateListingRequest.builder()
                .title("ab")
                .description("Valid description")
                .price(new BigDecimal("100.00"))
                .categoryId(1L)
                .condition(Condition.NEW)
                .build();
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(shortTitle);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("3")));

        // Test title too long
        CreateListingRequest longTitle = CreateListingRequest.builder()
                .title("a".repeat(201))
                .description("Valid description")
                .price(new BigDecimal("100.00"))
                .categoryId(1L)
                .condition(Condition.NEW)
                .build();
        violations = validator.validate(longTitle);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("200")));

        // Test valid title
        CreateListingRequest validRequest = CreateListingRequest.builder()
                .title("Valid Title")
                .description("Valid description")
                .price(new BigDecimal("100.00"))
                .categoryId(1L)
                .condition(Condition.NEW)
                .build();
        violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("CreateListingRequest validates price is positive")
    void createListingRequest_validatesPricePositive() {
        // Test negative price
        CreateListingRequest negativePrice = CreateListingRequest.builder()
                .title("Valid Title")
                .description("Valid description")
                .price(new BigDecimal("-100.00"))
                .categoryId(1L)
                .condition(Condition.NEW)
                .build();
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(negativePrice);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("positive")));

        // Test zero price
        CreateListingRequest zeroPrice = CreateListingRequest.builder()
                .title("Valid Title")
                .description("Valid description")
                .price(BigDecimal.ZERO)
                .categoryId(1L)
                .condition(Condition.NEW)
                .build();
        violations = validator.validate(zeroPrice);
        assertFalse(violations.isEmpty());

        // Test valid price
        CreateListingRequest validRequest = CreateListingRequest.builder()
                .title("Valid Title")
                .description("Valid description")
                .price(new BigDecimal("100.00"))
                .categoryId(1L)
                .condition(Condition.NEW)
                .build();
        violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("UpdateStatusRequest accepts only valid ListingStatus values")
    void updateStatusRequest_acceptsValidStatus() {
        // Test valid status values
        for (ListingStatus status : ListingStatus.values()) {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(status)
                    .build();
            Set<ConstraintViolation<UpdateStatusRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Status " + status + " should be valid");
        }

        // Test null status
        UpdateStatusRequest nullStatus = UpdateStatusRequest.builder()
                .status(null)
                .build();
        Set<ConstraintViolation<UpdateStatusRequest>> violations = validator.validate(nullStatus);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("ListingDetailResponse includes seller info")
    void listingDetailResponse_includesSellerInfo() {
        // Arrange
        ListingDetailResponse.SellerInfo seller = ListingDetailResponse.SellerInfo.builder()
                .id(1L)
                .displayName("Test User")
                .avatarUrl("/uploads/avatars/user_1.jpg")
                .memberSince(java.time.LocalDateTime.now().minusDays(30))
                .listingCount(5L)
                .build();

        ListingDetailResponse.ImageInfo image = ListingDetailResponse.ImageInfo.builder()
                .id(1L)
                .imageUrl("/uploads/listings/listing_1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();

        // Act
        ListingDetailResponse response = ListingDetailResponse.builder()
                .id(1L)
                .title("Test Listing")
                .description("Description")
                .price(new BigDecimal("100.00"))
                .condition(Condition.NEW)
                .status(ListingStatus.AVAILABLE)
                .seller(seller)
                .images(java.util.List.of(image))
                .build();

        // Assert
        assertNotNull(response.getSeller());
        assertEquals("Test User", response.getSeller().getDisplayName());
        assertEquals(5L, response.getSeller().getListingCount());
        assertNotNull(response.getImages());
        assertEquals(1, response.getImages().size());
    }

    @Test
    @DisplayName("UpdateListingRequest has all optional fields")
    void updateListingRequest_allFieldsOptional() {
        // Empty request should be valid (all fields optional)
        UpdateListingRequest emptyRequest = UpdateListingRequest.builder().build();
        Set<ConstraintViolation<UpdateListingRequest>> violations = validator.validate(emptyRequest);
        assertTrue(violations.isEmpty(), "All fields should be optional");

        // Request with some fields
        UpdateListingRequest partialRequest = UpdateListingRequest.builder()
                .title("New Title")
                .price(new BigDecimal("150.00"))
                .build();
        violations = validator.validate(partialRequest);
        assertTrue(violations.isEmpty());
    }
}