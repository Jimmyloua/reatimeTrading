package com.tradingplatform.content.service;

import com.tradingplatform.content.entity.CuratedCollection;
import com.tradingplatform.content.entity.CuratedCollectionItem;
import com.tradingplatform.content.entity.HomepageModule;
import com.tradingplatform.content.entity.HomepageModuleItem;
import com.tradingplatform.content.repository.CuratedCollectionRepository;
import com.tradingplatform.content.repository.HomepageModuleRepository;
import com.tradingplatform.listing.entity.Category;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.enums.ListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    private static final Path CHANGELOG_DIR = Path.of("src", "main", "resources", "db", "changelog");

    @Mock
    private CuratedCollectionRepository curatedCollectionRepository;

    @Mock
    private HomepageModuleRepository homepageModuleRepository;

    @InjectMocks
    private ContentService contentService;

    private Category camerasCategory;
    private Listing availableListingWithImage;
    private Listing availableListingWithoutImage;
    private Listing soldListing;
    private CuratedCollection curatedCollection;
    private HomepageModule heroModule;
    private HomepageModule tilesModule;

    @BeforeEach
    void setUp() {
        camerasCategory = Category.builder()
                .id(4L)
                .name("Cameras")
                .slug("cameras")
                .build();

        availableListingWithImage = listing(101L, "Sony A7 III", ListingStatus.AVAILABLE);
        availableListingWithImage.setImages(List.of(ListingImage.builder()
                .id(1001L)
                .listing(availableListingWithImage)
                .imagePath("sony-a7.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build()));

        availableListingWithoutImage = listing(102L, "Canon EOS RP", ListingStatus.AVAILABLE);
        availableListingWithoutImage.setImages(List.of());

        soldListing = listing(103L, "Fuji X-T3", ListingStatus.SOLD);
        soldListing.setImages(List.of(ListingImage.builder()
                .id(1002L)
                .listing(soldListing)
                .imagePath("fuji-xt3.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build()));

        curatedCollection = CuratedCollection.builder()
                .id(1L)
                .slug("featured-cameras")
                .title("Featured Cameras")
                .subtitle("Creator-ready gear")
                .description("Top camera picks")
                .coverImageUrl("/images/home/collections/featured-cameras.jpg")
                .targetType("category")
                .targetValue("4")
                .active(true)
                .displayOrder(2)
                .items(List.of(
                        collectionItem(availableListingWithoutImage, 2, "Fallback"),
                        collectionItem(soldListing, 3, "Sold"),
                        collectionItem(availableListingWithImage, 1, "Featured")
                ))
                .build();

        heroModule = HomepageModule.builder()
                .id(1L)
                .slug("home-hero")
                .moduleType("hero")
                .title("Hero")
                .displayOrder(2)
                .active(true)
                .items(List.of(moduleItem("hero-1.jpg", 1, "route", "/listings")))
                .build();

        tilesModule = HomepageModule.builder()
                .id(2L)
                .slug("home-image-tiles")
                .moduleType("image_tiles")
                .title("Tiles")
                .displayOrder(1)
                .active(true)
                .items(List.of(moduleItem("tile-1.jpg", 2, "route", "/listings?collection=student-laptops"),
                        moduleItem("tile-0.jpg", 1, "category", "4")))
                .build();
    }

    @Test
    @DisplayName("Task 1 changelogs exist and are registered in the Liquibase master file")
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
    @DisplayName("getActiveHomepageModules returns active modules ordered by displayOrder")
    void getActiveHomepageModulesReturnsOrderedModules() {
        when(homepageModuleRepository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(heroModule, tilesModule));

        List<ContentService.HomepageModuleContent> modules = contentService.getActiveHomepageModules();

        assertEquals(2, modules.size());
        assertEquals("home-image-tiles", modules.get(0).slug());
        assertEquals("home-hero", modules.get(1).slug());
        assertEquals(List.of("tile-0.jpg", "tile-1.jpg"),
                modules.get(0).items().stream().map(ContentService.HomepageModuleItemContent::imageUrl).toList());
        verify(homepageModuleRepository).findAllByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("getActiveCollectionBySlug filters unavailable listings and applies fallback cover images")
    void getActiveCollectionBySlugFiltersUnavailableListingsAndUsesFallbackImage() {
        when(curatedCollectionRepository.findBySlugAndActiveTrue("featured-cameras"))
                .thenReturn(Optional.of(curatedCollection));

        Optional<ContentService.CuratedCollectionContent> result =
                contentService.getActiveCollectionBySlug("featured-cameras");

        assertTrue(result.isPresent());
        ContentService.CuratedCollectionContent collection = result.orElseThrow();
        assertEquals("featured-cameras", collection.slug());
        assertEquals(2, collection.items().size());
        assertEquals(List.of(101L, 102L),
                collection.items().stream().map(ContentService.CollectionListingCard::listingId).toList());
        assertEquals("/uploads/listings/sony-a7.jpg", collection.items().get(0).imageUrl());
        assertFalse(collection.items().get(0).usesFallbackImage());
        assertEquals("/images/home/collections/featured-cameras.jpg", collection.items().get(1).imageUrl());
        assertTrue(collection.items().get(1).usesFallbackImage());
        verify(curatedCollectionRepository).findBySlugAndActiveTrue("featured-cameras");
    }

    private Listing listing(Long id, String title, ListingStatus status) {
        return Listing.builder()
                .id(id)
                .title(title)
                .description(title + " description")
                .price(new BigDecimal("499.99"))
                .status(status)
                .category(camerasCategory)
                .userId(1L)
                .deleted(false)
                .build();
    }

    private CuratedCollectionItem collectionItem(Listing listing, int displayOrder, String badge) {
        return CuratedCollectionItem.builder()
                .id((long) displayOrder)
                .listing(listing)
                .displayOrder(displayOrder)
                .badgeText(badge)
                .build();
    }

    private HomepageModuleItem moduleItem(String imageUrl, int displayOrder, String linkType, String linkValue) {
        return HomepageModuleItem.builder()
                .id((long) displayOrder)
                .imageUrl(imageUrl)
                .headline("Headline " + displayOrder)
                .subheadline("Subheadline " + displayOrder)
                .linkType(linkType)
                .linkValue(linkValue)
                .accentLabel("Accent " + displayOrder)
                .displayOrder(displayOrder)
                .build();
    }
}
