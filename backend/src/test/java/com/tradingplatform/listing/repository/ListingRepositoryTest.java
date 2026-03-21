package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.Category;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ListingRepositoryTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
        categoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("Repository can be injected and used")
    void repositoryInjection_works() {
        // Arrange & Act - Create and save a listing
        Listing listing = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("99.99"))
                .condition(Condition.NEW)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(testUserId)
                .build();
        Listing saved = listingRepository.save(listing);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("Test Listing", saved.getTitle());
    }

    @Test
    @DisplayName("findById returns saved listing")
    void findById_existingListing_returnsListing() {
        // Arrange
        Listing listing = Listing.builder()
                .title("iPhone 15")
                .description("Brand new iPhone")
                .price(new BigDecimal("999.99"))
                .condition(Condition.NEW)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(testUserId)
                .build();
        Listing saved = listingRepository.save(listing);

        // Act
        Listing found = listingRepository.findById(saved.getId()).orElse(null);

        // Assert
        assertNotNull(found);
        assertEquals("iPhone 15", found.getTitle());
        assertEquals(Condition.NEW, found.getCondition());
        assertEquals(ListingStatus.AVAILABLE, found.getStatus());
    }

    @Test
    @DisplayName("findByCategoryId returns listings in category")
    void findByCategoryId_existingCategory_returnsListings() {
        // Arrange
        Listing listing1 = Listing.builder()
                .title("Laptop")
                .description("Gaming laptop")
                .price(new BigDecimal("1500.00"))
                .condition(Condition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(testUserId)
                .build();
        Listing listing2 = Listing.builder()
                .title("Phone")
                .description("Smartphone")
                .price(new BigDecimal("500.00"))
                .condition(Condition.LIKE_NEW)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(testUserId)
                .build();
        listingRepository.save(listing1);
        listingRepository.save(listing2);

        // Act
        List<Listing> listings = listingRepository.findByCategoryId(testCategory.getId());

        // Assert
        assertEquals(2, listings.size());
    }

    @Test
    @DisplayName("findByUserId returns listings by user")
    void findByUserId_existingUser_returnsListings() {
        // Arrange
        Long anotherUserId = 2L;
        Listing listing1 = Listing.builder()
                .title("User 1 Item")
                .description("Item from user 1")
                .price(new BigDecimal("100.00"))
                .condition(Condition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(testUserId)
                .build();
        Listing listing2 = Listing.builder()
                .title("User 2 Item")
                .description("Item from user 2")
                .price(new BigDecimal("200.00"))
                .condition(Condition.NEW)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(anotherUserId)
                .build();
        listingRepository.save(listing1);
        listingRepository.save(listing2);

        // Act
        List<Listing> userListings = listingRepository.findByUserId(testUserId);

        // Assert
        assertEquals(1, userListings.size());
        assertEquals("User 1 Item", userListings.get(0).getTitle());
    }

    @Test
    @DisplayName("findByStatus returns listings with specific status")
    void findByStatus_availableStatus_returnsAvailableListings() {
        // Arrange
        Listing availableListing = Listing.builder()
                .title("Available Item")
                .description("Available for purchase")
                .price(new BigDecimal("100.00"))
                .condition(Condition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .category(testCategory)
                .userId(testUserId)
                .build();
        Listing soldListing = Listing.builder()
                .title("Sold Item")
                .description("Already sold")
                .price(new BigDecimal("50.00"))
                .condition(Condition.FAIR)
                .status(ListingStatus.SOLD)
                .category(testCategory)
                .userId(testUserId)
                .build();
        listingRepository.save(availableListing);
        listingRepository.save(soldListing);

        // Act
        List<Listing> availableListings = listingRepository.findByStatus(ListingStatus.AVAILABLE);

        // Assert
        assertEquals(1, availableListings.size());
        assertEquals("Available Item", availableListings.get(0).getTitle());
    }
}