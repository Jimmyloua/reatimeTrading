package com.tradingplatform.avatar;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class AvatarStorageService {

    @Value("${avatar.upload.dir:./uploads/avatars}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            log.info("Avatar upload directory initialized: {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create avatar upload directory", e);
            throw new RuntimeException("Failed to create avatar upload directory", e);
        }
    }

    /**
     * Stores a BufferedImage to the upload directory.
     */
    public void store(String filename, BufferedImage image, String format) {
        Path targetPath = uploadPath.resolve(filename);
        try {
            File file = targetPath.toFile();
            ImageIO.write(image, format, file);
            log.debug("Stored image: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to store image: {}", filename, e);
            throw new RuntimeException("Failed to store image", e);
        }
    }

    /**
     * Loads the path for a given filename.
     */
    public Path load(String filename) {
        return uploadPath.resolve(filename);
    }

    /**
     * Deletes a file from the upload directory.
     */
    public void delete(String filename) {
        Path file = load(filename);
        try {
            Files.deleteIfExists(file);
            log.debug("Deleted file: {}", file);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }

    /**
     * Checks if a file exists.
     */
    public boolean exists(String filename) {
        return Files.exists(load(filename));
    }

    /**
     * Gets the upload directory path.
     */
    public Path getUploadPath() {
        return uploadPath;
    }
}