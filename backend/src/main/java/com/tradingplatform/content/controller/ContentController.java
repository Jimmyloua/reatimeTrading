package com.tradingplatform.content.controller;

import com.tradingplatform.content.dto.CuratedCollectionResponse;
import com.tradingplatform.content.dto.HomepageResponse;
import com.tradingplatform.content.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/homepage")
    public ResponseEntity<HomepageResponse> getHomepage() {
        // Preserve active content ordering and displayOrder from the foundation service.
        return ResponseEntity.ok(contentService.getHomepage());
    }

    @GetMapping("/collections/{slug}")
    public ResponseEntity<CuratedCollectionResponse> getCollectionBySlug(@PathVariable String slug) {
        // Collections resolve only active content and keep displayOrder-stable item ordering.
        return contentService.getCollectionBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
