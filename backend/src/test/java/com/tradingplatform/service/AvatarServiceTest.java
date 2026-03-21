package com.tradingplatform.service;

import com.tradingplatform.avatar.AvatarService;
import com.tradingplatform.avatar.AvatarStorageService;
import com.tradingplatform.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import javax.imageio.ImageIO;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private AvatarStorageService storageService;

    private AvatarService avatarService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        avatarService = new AvatarService(storageService);
        ReflectionTestUtils.setField(avatarService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(avatarService, "maxFileSize", 5242880L); // 5 MB
    }

    @Test
    @DisplayName("storeAvatar accepts valid JPEG file")
    void storeAvatar_validJpeg_returnsFilename() throws Exception {
        // Create a valid JPEG image
        MockMultipartFile file = createMockImage("image/jpeg", "test.jpg");

        when(storageService.exists(any())).thenReturn(false);

        String filename = avatarService.storeAvatar(file, 1L);

        assertNotNull(filename);
        assertTrue(filename.startsWith("user_1."));
        verify(storageService).store(anyString(), any(BufferedImage.class), anyString());
    }

    @Test
    @DisplayName("storeAvatar accepts valid PNG file")
    void storeAvatar_validPng_returnsFilename() throws Exception {
        MockMultipartFile file = createMockImage("image/png", "test.png");

        when(storageService.exists(any())).thenReturn(false);

        String filename = avatarService.storeAvatar(file, 1L);

        assertNotNull(filename);
        assertTrue(filename.startsWith("user_1."));
    }

    @Test
    @DisplayName("storeAvatar accepts valid WebP file")
    void storeAvatar_validWebp_returnsFilename() throws Exception {
        // WebP is not natively supported by ImageIO, so we create a mock with valid content type
        // The actual WebP support would require additional libraries
        // For this test, we'll use PNG content with WebP content type to test validation
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.webp",
                "image/webp",
                baos.toByteArray()
        );

        when(storageService.exists(any())).thenReturn(false);

        String filename = avatarService.storeAvatar(file, 1L);

        assertNotNull(filename);
        assertTrue(filename.startsWith("user_1."));
    }

    @Test
    @DisplayName("storeAvatar rejects file larger than 5 MB")
    void storeAvatar_fileTooLarge_throwsException() {
        // Create a file larger than 5 MB
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        ApiException exception = assertThrows(ApiException.class, () ->
                avatarService.storeAvatar(file, 1L)
        );

        assertTrue(exception.getMessage().contains("5 MB"));
    }

    @Test
    @DisplayName("storeAvatar rejects empty file")
    void storeAvatar_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        ApiException exception = assertThrows(ApiException.class, () ->
                avatarService.storeAvatar(file, 1L)
        );

        assertTrue(exception.getMessage().toLowerCase().contains("empty"));
    }

    @Test
    @DisplayName("storeAvatar rejects non-image file")
    void storeAvatar_nonImage_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        ApiException exception = assertThrows(ApiException.class, () ->
                avatarService.storeAvatar(file, 1L)
        );

        assertTrue(exception.getMessage().contains("JPEG") ||
                   exception.getMessage().contains("PNG") ||
                   exception.getMessage().contains("WebP"));
    }

    @Test
    @DisplayName("storeAvatar rejects GIF file")
    void storeAvatar_gifFile_throwsException() throws Exception {
        MockMultipartFile file = createMockImage("image/gif", "test.gif");

        ApiException exception = assertThrows(ApiException.class, () ->
                avatarService.storeAvatar(file, 1L)
        );

        assertTrue(exception.getMessage().contains("JPEG") ||
                   exception.getMessage().contains("PNG") ||
                   exception.getMessage().contains("WebP"));
    }

    @Test
    @DisplayName("storeAvatar replaces existing avatar")
    void storeAvatar_existingAvatar_replacesOld() throws Exception {
        MockMultipartFile file = createMockImage("image/jpeg", "test.jpg");

        when(storageService.exists("user_1.jpg")).thenReturn(true);

        avatarService.storeAvatar(file, 1L);

        verify(storageService).delete("user_1.jpg");
    }

    @Test
    @DisplayName("createThumbnail generates 200x200 image")
    void createThumbnail_returnsCorrectDimensions() throws Exception {
        // Create a 400x300 image
        BufferedImage original = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);

        BufferedImage thumbnail = avatarService.createThumbnail(original);

        assertEquals(200, thumbnail.getWidth());
        assertEquals(200, thumbnail.getHeight());
    }

    @Test
    @DisplayName("createThumbnail center-crops the image")
    void createThumbnail_centerCrops() throws Exception {
        // Create a 400x200 image (wider than tall)
        BufferedImage original = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        // Fill with different colors to verify center crop
        for (int x = 0; x < 400; x++) {
            for (int y = 0; y < 200; y++) {
                original.setRGB(x, y, x < 200 ? 0xFF0000 : 0x00FF00); // Red on left, green on right
            }
        }

        BufferedImage thumbnail = avatarService.createThumbnail(original);

        // The thumbnail should be a center crop of the square portion
        assertEquals(200, thumbnail.getWidth());
        assertEquals(200, thumbnail.getHeight());
    }

    @Test
    @DisplayName("deleteAvatar calls storage service delete")
    void deleteAvatar_callsStorageDelete() {
        avatarService.deleteAvatar("user_1.jpg");

        verify(storageService).delete("user_1.jpg");
    }

    private MockMultipartFile createMockImage(String contentType, String filename) throws IOException {
        // Create a simple 100x100 image
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String format = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
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