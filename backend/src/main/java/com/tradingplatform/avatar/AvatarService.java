package com.tradingplatform.avatar;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
public class AvatarService {

    private final AvatarStorageService storageService;

    @Value("${avatar.upload.dir:./uploads/avatars}")
    private String uploadDir;

    @Value("${avatar.max-size:5242880}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final int THUMBNAIL_SIZE = 200;

    public AvatarService(AvatarStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Stores an avatar image after validation and thumbnail generation.
     * Implements D-10: Max file size 5 MB
     * Implements D-11: Only JPEG, PNG, WebP allowed
     * Implements D-12: Replaces existing avatar
     */
    public String storeAvatar(MultipartFile file, Long userId) {
        return storeAvatar(AvatarUpload.fromMultipartFile(file), userId);
    }

    public String storeAvatar(AvatarUpload file, Long userId) {
        validateFile(file);

        String filename = generateFilename(userId, file.contentType());

        if (storageService.exists(filename)) {
            storageService.delete(filename);
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(file.bytes()));
            if (original == null) {
                throw new ApiException(ErrorCode.INVALID_AVATAR, "Invalid image file");
            }

            BufferedImage thumbnail = createThumbnail(original);
            String format = getFormat(file.contentType());
            storageService.store(filename, thumbnail, format);

            log.info("Stored avatar for user {}: {}", userId, filename);
            return filename;
        } catch (IOException e) {
            log.error("Failed to store avatar for user {}", userId, e);
            throw new ApiException(ErrorCode.AVATAR_UPLOAD_FAILED, "Failed to store avatar");
        }
    }

    /**
     * Validates the uploaded file.
     * Implements D-10: File size validation
     * Implements D-11: Content type validation
     */
    public void validateFile(MultipartFile file) {
        validateFile(AvatarUpload.fromMultipartFile(file));
    }

    public void validateFile(AvatarUpload file) {
        if (file == null || file.size() == 0 || file.bytes().length == 0) {
            throw new ApiException(ErrorCode.INVALID_AVATAR, "File is empty");
        }

        if (file.size() > maxFileSize) {
            throw new ApiException(ErrorCode.INVALID_AVATAR,
                    "File exceeds maximum size of 5 MB");
        }

        String contentType = file.contentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException(ErrorCode.INVALID_AVATAR,
                    "Only JPEG, PNG, and WebP images are allowed");
        }
    }

    /**
     * Creates a 200x200 thumbnail from the original image.
     * Uses center-crop to maintain aspect ratio.
     */
    public BufferedImage createThumbnail(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Calculate the square crop dimensions (center crop)
        int minDimension = Math.min(width, height);
        int x = (width - minDimension) / 2;
        int y = (height - minDimension) / 2;

        // Crop to square
        BufferedImage cropped = original.getSubimage(x, y, minDimension, minDimension);

        // Resize to 200x200
        BufferedImage thumbnail = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE, null);
        g.dispose();

        return thumbnail;
    }

    /**
     * Deletes an avatar file.
     */
    public void deleteAvatar(String filename) {
        if (filename != null && !filename.isBlank()) {
            storageService.delete(filename);
            log.info("Deleted avatar: {}", filename);
        }
    }

    private String generateFilename(Long userId, String contentType) {
        String extension = getFormat(contentType);
        return String.format("user_%d.%s", userId, extension);
    }

    private String getFormat(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    public record AvatarUpload(byte[] bytes, long size, String contentType) {

        public static AvatarUpload fromMultipartFile(MultipartFile file) {
            if (file == null) {
                return new AvatarUpload(new byte[0], 0, null);
            }
            try {
                return new AvatarUpload(file.getBytes(), file.getSize(), file.getContentType());
            } catch (IOException exception) {
                throw new ApiException(ErrorCode.AVATAR_UPLOAD_FAILED, "Failed to read avatar upload");
            }
        }
    }
}
