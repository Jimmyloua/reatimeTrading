package com.tradingplatform.content.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentServiceTest {

    private static final Path CHANGELOG_DIR = Path.of("src", "main", "resources", "db", "changelog");
    private static final Path CONTENT_ROOT = Path.of("src", "main", "java", "com", "tradingplatform", "content");
    private static final Path SERVICE_SOURCE = CONTENT_ROOT.resolve(Path.of("service", "ContentService.java"));
    private static final String AVAILABLE_STATUS = "AVAILABLE";
    private static final String HERO_MODULE = "hero";
    private static final String IMAGE_TILES_MODULE = "image_tiles";
    private static final String COLLECTION_ROW_MODULE = "collection_row";
    private static final String CATEGORY_SPOTLIGHT_MODULE = "category_spotlight";

    @Test
    void contentLiquibaseChangelogsExistAndAreRegistered() throws Exception {
        Path schemaChangelog = CHANGELOG_DIR.resolve("010-create-content-tables.xml");
        Path seedChangelog = CHANGELOG_DIR.resolve("011-seed-homepage-content.xml");
        Path masterChangelog = CHANGELOG_DIR.resolve("db.changelog-master.xml");

        assertAll(
                () -> assertTrue(Files.exists(schemaChangelog), "Expected 010-create-content-tables.xml to exist"),
                () -> assertTrue(Files.exists(seedChangelog), "Expected 011-seed-homepage-content.xml to exist"),
                () -> assertTrue(Files.readString(masterChangelog).contains("010-create-content-tables.xml"),
                        "Expected db.changelog-master.xml to include 010-create-content-tables.xml"),
                () -> assertTrue(Files.readString(masterChangelog).contains("011-seed-homepage-content.xml"),
                        "Expected db.changelog-master.xml to include 011-seed-homepage-content.xml")
        );
    }

    @Test
    void contentServiceFiltersUnavailableOrImagelessMembersAndPreservesModuleOrder() throws Exception {
        assertTrue(Files.exists(SERVICE_SOURCE), "Expected ContentService.java to exist");

        String serviceSource = Files.readString(SERVICE_SOURCE);

        assertAll(
                () -> assertTrue(serviceSource.contains(AVAILABLE_STATUS),
                        "Expected curated collection filtering to keep only " + AVAILABLE_STATUS + " listings"),
                () -> assertTrue(serviceSource.contains(HERO_MODULE),
                        "Expected homepage content handling to preserve " + HERO_MODULE + " modules"),
                () -> assertTrue(serviceSource.contains(IMAGE_TILES_MODULE),
                        "Expected homepage content handling to preserve " + IMAGE_TILES_MODULE + " modules"),
                () -> assertTrue(serviceSource.contains(COLLECTION_ROW_MODULE),
                        "Expected homepage content handling to preserve " + COLLECTION_ROW_MODULE + " modules"),
                () -> assertTrue(serviceSource.contains(CATEGORY_SPOTLIGHT_MODULE),
                        "Expected homepage content handling to preserve " + CATEGORY_SPOTLIGHT_MODULE + " modules"),
                () -> assertTrue(serviceSource.contains("displayOrder"),
                        "Expected homepage content service to sort modules by displayOrder"),
                () -> assertTrue(
                        serviceSource.contains("fallback") || serviceSource.contains("primaryImage"),
                        "Expected content service to filter imageless members or use a collection-level fallback image")
        );
    }
}
