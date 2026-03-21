package com.tradingplatform.listing.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Service for listing storage operations (image file storage).
 * Stub for Wave 0 - will be implemented in Plan 02-02.
 */
@Service
public class ListingStorageService {

    private Path uploadDir;

    public String store(MultipartFile file, Long listingId) {
        // TODO: Implement in Plan 02-02
        String filename = generateFilename(file.getOriginalFilename());
        return filename;
    }

    public void delete(String filename) {
        // TODO: Implement in Plan 02-02
    }

    public boolean exists(String filename) {
        // TODO: Implement in Plan 02-02
        return false;
    }

    private String generateFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}