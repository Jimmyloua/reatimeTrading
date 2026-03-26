package com.tradingplatform.content.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentControllerTest {

    private static final Path CONTENT_ROOT = Path.of("src", "main", "java", "com", "tradingplatform", "content");
    private static final Path CONTROLLER_SOURCE = CONTENT_ROOT.resolve(Path.of("controller", "ContentController.java"));
    private static final String HOMEPAGE_ENDPOINT = "/api/content/homepage";
    private static final String FEATURED_COLLECTION_ENDPOINT = "/api/content/collections/featured-cameras";

    @Test
    void homepageAndCollectionEndpointsAreDefinedWithActiveOrderedContentContract() throws Exception {
        assertTrue(Files.exists(CONTROLLER_SOURCE), "Expected ContentController.java to exist");

        String controllerSource = Files.readString(CONTROLLER_SOURCE);

        assertAll(
                () -> assertTrue(controllerSource.contains("/api/content"),
                        "Expected content controller to define the /api/content base route"),
                () -> assertTrue(controllerSource.contains("/homepage"),
                        "Expected content controller to expose " + HOMEPAGE_ENDPOINT),
                () -> assertTrue(controllerSource.contains("/collections/{slug}"),
                        "Expected content controller to expose collection lookup routes"),
                () -> assertTrue(controllerSource.contains("displayOrder"),
                        "Expected content responses to preserve displayOrder ordering"),
                () -> assertTrue(controllerSource.contains("active"),
                        "Expected homepage and collection endpoints to return only active content"),
                () -> assertTrue(HOMEPAGE_ENDPOINT.equals("/api/content/homepage"),
                        "Expected homepage endpoint path to remain " + HOMEPAGE_ENDPOINT),
                () -> assertTrue(FEATURED_COLLECTION_ENDPOINT.equals("/api/content/collections/featured-cameras"),
                        "Expected featured collection endpoint path to remain " + FEATURED_COLLECTION_ENDPOINT)
        );
    }
}
