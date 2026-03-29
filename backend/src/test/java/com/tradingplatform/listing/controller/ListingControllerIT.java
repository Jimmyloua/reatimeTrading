package com.tradingplatform.listing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.auth.dto.RegisterRequest;
import com.tradingplatform.listing.dto.CreateListingRequest;
import com.tradingplatform.listing.dto.UpdateStatusRequest;
import com.tradingplatform.listing.entity.Category;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.repository.CategoryRepository;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ListingController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private String testUserToken;
    private String otherUserToken;
    private User testUser;
    private User otherUser;
    private Category testCategory;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Register test user
        RegisterRequest testUserRequest = new RegisterRequest("test@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserRequest)))
                .andExpect(status().isOk())
                .andReturn();
        testUserToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        testUser = userRepository.findByEmail("test@example.com").orElseThrow();

        // Register other user
        RegisterRequest otherUserRequest = new RegisterRequest("other@example.com", "password123");
        result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUserRequest)))
                .andExpect(status().isOk())
                .andReturn();
        otherUserToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        otherUser = userRepository.findByEmail("other@example.com").orElseThrow();

        // Create test category
        testCategory = Category.builder()
                .name("Electronics")
                .slug("electronics-test")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("GET /api/listings allows anonymous users to browse listings")
    void testSearchListingsAllowsAnonymousUsers() throws Exception {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Anonymous Browse Listing")
                .description("Visible to logged-out users")
                .price(new BigDecimal("300.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .city("Shanghai")
                .build();

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/listings")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Anonymous Browse Listing"));
    }

    @Test
    @DisplayName("GET /api/listings/categories allows anonymous users")
    void testGetCategoryTreeAllowsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/listings/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/listings/categories/{id} allows anonymous users")
    void testGetCategoryByIdAllowsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/listings/categories/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCategory.getId()))
                .andExpect(jsonPath("$.slug").value("electronics-test"));
    }

    @Test
    @DisplayName("POST /api/listings requires authentication")
    void testCreateListingRequiresAuthentication() throws Exception {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Blocked Listing")
                .description("Should stay protected")
                .price(new BigDecimal("100.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .build();

        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/listings creates a new listing")
    void testCreateListingEndpoint() throws Exception {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("iPhone 15 Pro")
                .description("Brand new iPhone")
                .price(new BigDecimal("999.99"))
                .categoryId(testCategory.getId())
                .condition(Condition.NEW)
                .city("New York")
                .build();

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.condition").value("NEW"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/listings validates required fields")
    void testCreateListingValidation() throws Exception {
        String invalidRequest = "{}";

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/listings/{id} returns listing details")
    void testGetListingEndpoint() throws Exception {
        // First create a listing
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Test Phone")
                .description("Test description")
                .price(new BigDecimal("500.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract listing ID
        String response = createResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        Long listingId = jsonNode.get("id").asLong();

        // Get listing details (public endpoint)
        mockMvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Phone"))
                .andExpect(jsonPath("$.description").value("Test description"))
                .andExpect(jsonPath("$.seller.displayName").value("New User"));
    }

    @Test
    @DisplayName("GET /api/listings allows anonymous users to browse listings")
    void testSearchListingsEndpointAllowsAnonymousAccess() throws Exception {
        createListing("Anonymous Browse Listing", testUserToken);

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Anonymous Browse Listing"));
    }

    @Test
    @DisplayName("GET /api/listings/categories allows anonymous users to read category tree")
    void testGetCategoryTreeAllowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/listings/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testCategory.getId()))
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/listings/categories/{id} allows anonymous users to read category details")
    void testGetCategoryByIdAllowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/listings/categories/{id}", testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCategory.getId()))
                .andExpect(jsonPath("$.slug").value("electronics-test"));
    }

    @Test
    @DisplayName("POST /api/listings without authentication returns 401")
    void testCreateListingWithoutAuthenticationReturnsUnauthorized() throws Exception {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Unauthorized Listing")
                .description("Should fail")
                .price(new BigDecimal("199.99"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .build();

        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/listings/{id}/status without authentication returns 401")
    void testUpdateStatusWithoutAuthenticationReturnsUnauthorized() throws Exception {
        Long listingId = createListing("Protected Status Listing", testUserToken);

        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(ListingStatus.SOLD)
                .build();

        mockMvc.perform(patch("/api/listings/{id}/status", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/listings/{id} updates a listing for owner")
    void testUpdateListingEndpoint() throws Exception {
        // First create a listing
        CreateListingRequest createRequest = CreateListingRequest.builder()
                .title("Original Title")
                .description("Original description")
                .price(new BigDecimal("100.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long listingId = objectMapper.readTree(response).get("id").asLong();

        // Update the listing
        String updateRequest = """
            {
                "title": "Updated Title",
                "price": 150.00
            }
            """;

        mockMvc.perform(put("/api/listings/" + listingId)
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.price").value(150.00));
    }

    @Test
    @DisplayName("PUT /api/listings/{id} returns 403 for non-owner")
    void testUpdateListingNonOwner() throws Exception {
        // Create listing as test user
        CreateListingRequest createRequest = CreateListingRequest.builder()
                .title("Original Title")
                .description("Original description")
                .price(new BigDecimal("100.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long listingId = objectMapper.readTree(response).get("id").asLong();

        // Try to update as other user
        String updateRequest = """
            {
                "title": "Hacked Title"
            }
            """;

        mockMvc.perform(put("/api/listings/" + listingId)
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/listings/{id} deletes a listing for owner")
    void testDeleteListingEndpoint() throws Exception {
        // Create listing
        CreateListingRequest createRequest = CreateListingRequest.builder()
                .title("To Delete")
                .description("Will be deleted")
                .price(new BigDecimal("50.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.FAIR)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long listingId = objectMapper.readTree(response).get("id").asLong();

        // Delete the listing
        mockMvc.perform(delete("/api/listings/" + listingId)
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNoContent());

        // Verify it's deleted (returns 404)
        mockMvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/listings/{id}/status updates status for owner")
    void testUpdateStatusEndpoint() throws Exception {
        // Create listing
        CreateListingRequest createRequest = CreateListingRequest.builder()
                .title("Status Test")
                .description("Test status update")
                .price(new BigDecimal("100.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.NEW)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long listingId = objectMapper.readTree(response).get("id").asLong();

        // Update status to SOLD
        UpdateStatusRequest statusRequest = UpdateStatusRequest.builder()
                .status(ListingStatus.SOLD)
                .build();

        mockMvc.perform(patch("/api/listings/" + listingId + "/status")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    @DisplayName("GET /api/listings/user/{userId} returns user listings")
    void testGetUserListingsEndpoint() throws Exception {
        // Create listings for test user
        for (int i = 0; i < 3; i++) {
            CreateListingRequest request = CreateListingRequest.builder()
                    .title("User Listing " + i)
                    .description("Description " + i)
                    .price(new BigDecimal("100.00"))
                    .categoryId(testCategory.getId())
                    .condition(Condition.GOOD)
                    .build();

            mockMvc.perform(post("/api/listings")
                            .header("Authorization", "Bearer " + testUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get user listings
        mockMvc.perform(get("/api/listings/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("GET /api/listings/{id} returns 404 for non-existent listing")
    void testGetListingNotFound() throws Exception {
        mockMvc.perform(get("/api/listings/999999"))
                .andExpect(status().isNotFound());
    }

    private Long createListing(String title, String token) throws Exception {
        CreateListingRequest request = CreateListingRequest.builder()
                .title(title)
                .description("Visible to logged-out users")
                .price(new BigDecimal("300.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .city("Shanghai")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }
}
