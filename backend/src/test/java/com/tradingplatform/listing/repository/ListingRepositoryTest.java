package com.tradingplatform.listing.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test stubs for LIST-01,02,04,05 (Listing repository operations).
 * These tests will be implemented in Plan 02-01 and 02-02.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
class ListingRepositoryTest {

    @Autowired
    private ListingRepository listingRepository;

    @Test
    @DisplayName("findByCategory returns listings in given category")
    void testFindByCategory() {
        // TODO: Implement in Plan 02-01
    }

    @Test
    @DisplayName("Full-text search returns matching listings")
    void testFullTextSearch() {
        // TODO: Implement in Plan 02-02
    }

    @Test
    @DisplayName("Location filter returns listings within radius")
    void testLocationFilter() {
        // TODO: Implement in Plan 02-03
    }

    @Test
    @DisplayName("Price range filter returns listings within price bounds")
    void testPriceRangeFilter() {
        // TODO: Implement in Plan 02-02
    }
}