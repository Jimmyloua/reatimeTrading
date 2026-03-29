package com.tradingplatform.listing.controller;

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
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
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

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private String testUserToken;
    private String otherUserToken;
    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        testUserToken = registerUser("test@example.com");
        otherUserToken = registerUser("other@example.com");
        testUser = userRepository.findByEmail("test@example.com").orElseThrow();
        testCategory = categoryRepository.save(buildCategory());
    }

    @Test
    @DisplayName("POST /api/listings creates a new listing")
    void createListing_authenticated_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .header("Authorization", bearer(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildListingRequest("iPhone 15 Pro"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/listings validates required fields")
    void createListing_invalidPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .header("Authorization", bearer(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/listings allows anonymous users to browse listings")
    void searchListings_anonymous_returnsResults() throws Exception {
        createListing("Anonymous Browse Listing", testUserToken);

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Anonymous Browse Listing"));
    }

    @Test
    @DisplayName("GET /api/listings/categories allows anonymous users")
    void getCategoryTree_anonymous_returnsTree() throws Exception {
        mockMvc.perform(get("/api/listings/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testCategory.getId()))
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/listings/categories/{id} allows anonymous users")
    void getCategoryById_anonymous_returnsCategory() throws Exception {
        mockMvc.perform(get("/api/listings/categories/{id}", testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("electronics-test"));
    }

    @Test
    @DisplayName("GET /api/listings/{id} returns listing details")
    void getListingDetail_anonymous_returnsListing() throws Exception {
        Long listingId = createListing("Test Phone", testUserToken);

        mockMvc.perform(get("/api/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Phone"))
                .andExpect(jsonPath("$.seller.displayName").value("New User"));
    }

    @Test
    @DisplayName("GET /api/listings/{id} returns 404 for non-existent listing")
    void getListingDetail_missingListing_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/listings/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/listings/{id} updates a listing for owner")
    void updateListing_owner_returnsUpdatedListing() throws Exception {
        Long listingId = createListing("Original Title", testUserToken);

        mockMvc.perform(put("/api/listings/{id}", listingId)
                        .header("Authorization", bearer(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "price": 150.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.price").value(150.00));
    }

    @Test
    @DisplayName("PUT /api/listings/{id} returns 403 for non-owner")
    void updateListing_nonOwner_returnsForbidden() throws Exception {
        Long listingId = createListing("Original Title", testUserToken);

        mockMvc.perform(put("/api/listings/{id}", listingId)
                        .header("Authorization", bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/listings/{id} deletes a listing for owner")
    void deleteListing_owner_softDeletesListing() throws Exception {
        Long listingId = createListing("To Delete", testUserToken);

        mockMvc.perform(delete("/api/listings/{id}", listingId)
                        .header("Authorization", bearer(testUserToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/listings/{id}", listingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/listings/{id}/status updates status for owner")
    void updateStatus_owner_returnsUpdatedStatus() throws Exception {
        Long listingId = createListing("Status Test", testUserToken);
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(ListingStatus.SOLD)
                .build();

        mockMvc.perform(patch("/api/listings/{id}/status", listingId)
                        .header("Authorization", bearer(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    @DisplayName("GET /api/listings/user/{userId} returns user listings")
    void getUserListings_anonymous_returnsPaginatedListings() throws Exception {
        createListing("User Listing 1", testUserToken);
        createListing("User Listing 2", testUserToken);
        createListing("User Listing 3", testUserToken);

        mockMvc.perform(get("/api/listings/user/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("POST /api/listings requires authentication")
    void createListing_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildListingRequest("Blocked Listing"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/listings/{id}/status requires authentication")
    void updateStatus_withoutAuthentication_returnsUnauthorized() throws Exception {
        Long listingId = createListing("Protected Status Listing", testUserToken);
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(ListingStatus.SOLD)
                .build();

        mockMvc.perform(patch("/api/listings/{id}/status", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private Category buildCategory() {
        return Category.builder()
                .name("Electronics")
                .slug("electronics-test")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
    }

    private CreateListingRequest buildListingRequest(String title) {
        return CreateListingRequest.builder()
                .title(title)
                .description("Visible to logged-out users")
                .price(new BigDecimal("300.00"))
                .categoryId(testCategory.getId())
                .condition(Condition.GOOD)
                .city("Shanghai")
                .build();
    }

    private Long createListing(String title, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildListingRequest(title))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private String registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
