package com.tradingplatform.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.auth.dto.RegisterRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private User ratedUser;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        ratedUser = registerUser("rated@example.com");
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

    private User registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
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
}
