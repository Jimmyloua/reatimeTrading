package com.tradingplatform.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.auth.dto.RegisterRequest;
import com.tradingplatform.listing.entity.Category;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.repository.CategoryRepository;
import com.tradingplatform.listing.repository.ListingRepository;
import com.tradingplatform.transaction.entity.Transaction;
import com.tradingplatform.transaction.entity.TransactionStatus;
import com.tradingplatform.transaction.repository.TransactionRepository;
import com.tradingplatform.transaction.dto.RatingRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private User ratedUser;
    private User buyerUser;
    private String buyerAccessToken;
    private Transaction completedTransaction;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        listingRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        ratedUser = registerUser("rated@example.com");
        buyerUser = registerUser("buyer@example.com");
        buyerAccessToken = login("buyer@example.com");
        completedTransaction = createCompletedTransaction();
    }

    @Test
    @DisplayName("GET /api/ratings/users/{userId} allows anonymous users to read public ratings")
    void getUserRatings_anonymous_returnsOk() throws Exception {
        mockMvc.perform(get("/api/ratings/users/{userId}", ratedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/ratings/users/{userId}/recent allows anonymous users to read recent ratings")
    void getRecentRatings_anonymous_returnsOk() throws Exception {
        mockMvc.perform(get("/api/ratings/users/{userId}/recent", ratedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/ratings/users/{userId}/summary allows anonymous users to read rating summary")
    void getRatingSummary_anonymous_returnsOk() throws Exception {
        mockMvc.perform(get("/api/ratings/users/{userId}/summary", ratedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ratedUser.getId()))
                .andExpect(jsonPath("$.hasRatings").value(false));
    }

    @Test
    @DisplayName("POST /api/ratings/transactions/{transactionId} without authentication returns 401")
    void submitRating_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/ratings/transactions/{transactionId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "reviewText": "Great trade"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/ratings/transactions/{transactionId}/can-rate without authentication returns 401")
    void canRate_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/ratings/transactions/{transactionId}/can-rate", 999L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/ratings/transactions/{transactionId}/can-rate returns true for eligible participant")
    void canRate_authenticatedEligibleParticipant_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/ratings/transactions/{transactionId}/can-rate", completedTransaction.getId())
                        .header("Authorization", "Bearer " + buyerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRate").value(true));
    }

    private User registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "password123");
        MvcResult result = performResolved(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(response)
                .get("userId")
                .asLong();
        return userRepository.findById(userId).orElseThrow();
    }

    private String login(String email) throws Exception {
        MvcResult result = performResolved(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private ResultActions performResolved(RequestBuilder requestBuilder) throws Exception {
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        MvcResult mvcResult = resultActions.andReturn();
        if (!mvcResult.getRequest().isAsyncStarted()) {
            return resultActions;
        }
        if (mvcResult.getRequest().getUserPrincipal() instanceof Authentication authenticationToken) {
            try {
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                return mockMvc.perform(asyncDispatch(mvcResult));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
        return mockMvc.perform(asyncDispatch(mvcResult));
    }

    private Transaction createCompletedTransaction() {
        Category category = categoryRepository.save(Category.builder()
                .slug("phones")
                .name("Phones")
                .description("Phones")
                .displayOrder(0)
                .build());

        Listing listing = listingRepository.save(Listing.builder()
                .title("Completed Listing")
                .description("Test completed listing")
                .price(new BigDecimal("199.99"))
                .condition(Condition.GOOD)
                .status(ListingStatus.SOLD)
                .category(category)
                .userId(ratedUser.getId())
                .city("Shanghai")
                .region("Shanghai")
                .deleted(false)
                .build());

        return transactionRepository.save(Transaction.builder()
                .listingId(listing.getId())
                .buyerId(buyerUser.getId())
                .sellerId(ratedUser.getId())
                .amount(listing.getPrice())
                .status(TransactionStatus.COMPLETED)
                .confirmedAt(LocalDateTime.now().minusDays(1))
                .settledAt(LocalDateTime.now().minusHours(20))
                .completedAt(LocalDateTime.now().minusHours(12))
                .build());
    }
}
