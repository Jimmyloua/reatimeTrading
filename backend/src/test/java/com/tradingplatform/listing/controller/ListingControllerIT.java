package com.tradingplatform.listing.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test stubs for LIST-01 to 08 (Listing controller endpoints).
 * These tests will be implemented in Plan 02-02.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
class ListingControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("POST /api/listings creates a new listing")
    void testCreateListingEndpoint() {
        // TODO: Implement in Plan 02-02
    }

    @Test
    @DisplayName("GET /api/listings/{id} returns listing details")
    void testGetListingEndpoint() {
        // TODO: Implement in Plan 02-02
    }

    @Test
    @DisplayName("PUT /api/listings/{id} updates a listing")
    void testUpdateListingEndpoint() {
        // TODO: Implement in Plan 02-02
    }

    @Test
    @DisplayName("DELETE /api/listings/{id} deletes a listing")
    void testDeleteListingEndpoint() {
        // TODO: Implement in Plan 02-02
    }

    @Test
    @DisplayName("GET /api/listings/search returns matching listings")
    void testSearchListingsEndpoint() {
        // TODO: Implement in Plan 02-03
    }
}