package com.tradingplatform.listing.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for listing image file storage.
 */
@Slf4j
@Service
public class ListingStorageService {

    @Value("${listing.upload.dir:./uploads/listings}")
    private String uploadDir;

    private Path uploadPath;

    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            log.info("Listing upload directory initialized: {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create listing upload directory", e);
            throw new RuntimeException("Failed to create listing upload directory", e);
        }
    }

    /**
     * Stores an image file after resizing to max 1200x1200.
     */
    public String store(MultipartFile file, Long listingId, int order, String format) {
        String filename = generateFilename(listingId, order, format);
        Path targetPath = uploadPath.resolve(filename);

        try {
            // Read original image
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                throw new RuntimeException("Invalid image file");
            }

            // Resize if needed
            BufferedImage resized = resizeImage(original);

            // Store the image
            File targetFile = targetPath.toFile();
            ImageIO.write(resized, format, targetFile);
            log.debug("Stored listing image: {}", targetPath);

            return filename;
        } catch (IOException e) {
            log.error("Failed to store listing image: {}", filename, e);
            throw new RuntimeException("Failed to store image", e);
        }
    }

    public Mono<String> storeReactive(MultipartFile file, Long listingId, int order, String format) {
        return Mono.fromCallable(() -> store(file, listingId, order, format))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes an image file.
     */
    public void delete(String filename) {
        Path file = uploadPath.resolve(filename);
        try {
            Files.deleteIfExists(file);
            log.debug("Deleted listing image: {}", file);
        } catch (IOException e) {
            log.warn("Failed to delete listing image: {}", filename, e);
        }
    }

    public Mono<Void> deleteReactive(String filename) {
        return Mono.fromRunnable(() -> delete(filename))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Checks if an image file exists.
     */
    public boolean exists(String filename) {
        return Files.exists(uploadPath.resolve(filename));
    }

    /**
     * Gets the upload directory path.
     */
    public Path getUploadPath() {
        return uploadPath;
    }

    /**
     * Generates a unique filename for a listing image.
     */
    private String generateFilename(Long listingId, int order, String format) {
        long timestamp = System.currentTimeMillis();
        return String.format("listing_%d_%d_%d.%s", listingId, timestamp, order, format);
    }

    /**
     * Resizes an image to fit within max dimensions while maintaining aspect ratio.
     */
    private BufferedImage resizeImage(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Check if resize is needed
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return original;
        }

        // Calculate new dimensions maintaining aspect ratio
        double widthRatio = (double) MAX_WIDTH / width;
        double heightRatio = (double) MAX_HEIGHT / height;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        // Create resized image
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }
}
