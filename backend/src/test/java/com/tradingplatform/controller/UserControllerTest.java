package com.tradingplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.auth.dto.LoginRequest;
import com.tradingplatform.auth.dto.RegisterRequest;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import com.tradingplatform.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Register a test user
        RegisterRequest registerRequest = new RegisterRequest("profile@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("accessToken").asText();
        testUser = userRepository.findByEmail("profile@example.com").orElseThrow();
    }

    @Test
    @DisplayName("GET /api/users/me returns current user profile")
    void getProfile_authenticated_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.displayName").value("New User")) // D-08: fallback
                .andExpect(jsonPath("$.avatarUrl").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.listingCount").value(0))
                .andExpect(jsonPath("$.isOwnProfile").value(true));
    }

    @Test
    @DisplayName("GET /api/users/me without auth returns 401")
    void getProfile_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/users/me updates display name")
    void updateProfile_validDisplayName_updatesSuccessfully() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Test User");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.isOwnProfile").value(true));

        // Verify profile is now complete
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assert updatedUser.isProfileComplete();
    }

    @Test
    @DisplayName("PUT /api/users/me sets profileComplete to true when display name is set")
    void updateProfile_setsProfileComplete() throws Exception {
        // Initially profile should not be complete
        assert !testUser.isProfileComplete();

        UpdateProfileRequest request = new UpdateProfileRequest("Complete User");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assert updatedUser.isProfileComplete();
        assert "Complete User".equals(updatedUser.getDisplayName());
    }

    @Test
    @DisplayName("GET /api/users/{id} returns public profile for existing user")
    void getPublicProfile_existingUser_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.displayName").value("New User"))
                .andExpect(jsonPath("$.avatarUrl").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.listingCount").value(0))
                .andExpect(jsonPath("$.isOwnProfile").value(true));
    }

    @Test
    @DisplayName("GET /api/users/{id} returns 404 for non-existent user")
    void getPublicProfile_nonExistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/users/{id} can be accessed without authentication")
    void getPublicProfile_unauthenticated_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.isOwnProfile").value(false));
    }

    @Test
    @DisplayName("PUT /api/users/me validates display name max length")
    void updateProfile_displayNameTooLong_returnsBadRequest() throws Exception {
        // Create a display name that exceeds 100 characters
        String longName = "a".repeat(101);
        UpdateProfileRequest request = new UpdateProfileRequest(longName);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/users/{id} shows different user profile with isOwnProfile=false")
    void getPublicProfile_otherUser_returnsProfileWithOwnProfileFalse() throws Exception {
        // Create another user
        User otherUser = User.builder()
                .email("other@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Other User")
                .profileComplete(true)
                .build();
        otherUser = userRepository.save(otherUser);

        mockMvc.perform(get("/api/users/{id}", otherUser.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(otherUser.getId()))
                .andExpect(jsonPath("$.displayName").value("Other User"))
                .andExpect(jsonPath("$.isOwnProfile").value(false));
    }
}