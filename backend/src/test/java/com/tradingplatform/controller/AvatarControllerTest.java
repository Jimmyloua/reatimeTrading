package com.tradingplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.auth.dto.RegisterRequest;
import com.tradingplatform.user.User;
import com.tradingplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AvatarControllerTest {

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
        RegisterRequest registerRequest = new RegisterRequest("avatar@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("accessToken").asText();
        testUser = userRepository.findByEmail("avatar@example.com").orElseThrow();
    }

    @Test
    @DisplayName("POST /api/users/me/avatar with valid JPEG returns 200")
    void uploadAvatar_validJpeg_returns200() throws Exception {
        MockMultipartFile file = createMockImage("image/jpeg", "avatar.jpg");

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/users/me/avatar with valid PNG returns 200")
    void uploadAvatar_validPng_returns200() throws Exception {
        MockMultipartFile file = createMockImage("image/png", "avatar.png");

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/users/me/avatar with invalid file returns 400")
    void uploadAvatar_invalidFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users/me/avatar without auth returns 401")
    void uploadAvatar_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = createMockImage("image/jpeg", "avatar.jpg");

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/users/me/avatar removes avatar")
    void deleteAvatar_existingAvatar_returns204() throws Exception {
        // First upload an avatar
        MockMultipartFile file = createMockImage("image/jpeg", "avatar.jpg");
        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Then delete it
        mockMvc.perform(delete("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Verify avatar path is null
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assert user.getAvatarPath() == null;
    }

    @Test
    @DisplayName("DELETE /api/users/me/avatar without existing avatar returns 204")
    void deleteAvatar_noAvatar_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Avatar path is stored in user record after upload")
    void uploadAvatar_storesPathInUserRecord() throws Exception {
        MockMultipartFile file = createMockImage("image/jpeg", "avatar.jpg");

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assert user.getAvatarPath() != null;
        assert user.getAvatarPath().startsWith("user_" + testUser.getId());
    }

    @Test
    @DisplayName("Avatar upload rejects file over 5 MB")
    void uploadAvatar_fileTooLarge_returns400() throws Exception {
        // Create a file larger than 5 MB
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    private MockMultipartFile createMockImage(String contentType, String filename) throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String format = switch (contentType) {
            case "image/png" -> "png";
            default -> "jpg";
        };

        ImageIO.write(image, format, baos);

        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                baos.toByteArray()
        );
    }
}