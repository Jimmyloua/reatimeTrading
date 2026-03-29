package com.tradingplatform.content.controller;

import com.tradingplatform.content.dto.CuratedCollectionResponse;
import com.tradingplatform.content.dto.HomepageResponse;
import com.tradingplatform.content.service.ContentService;
import com.tradingplatform.listing.dto.ListingResponse;
import com.tradingplatform.listing.enums.Condition;
import com.tradingplatform.listing.enums.ListingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContentService contentService;

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Test
    @DisplayName("GET /api/content/homepage allows anonymous users to load homepage content")
    void getHomepage_anonymous_returnsHomepageModules() throws Exception {
        when(contentService.getHomepage()).thenReturn(buildHomepageResponse());

        mockMvc.perform(get("/api/content/homepage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].slug").value("home-hero"))
                .andExpect(jsonPath("$.modules[0].items[0].linkValue").value("/listings"));
    }

    @Test
    @DisplayName("GET /api/content/collections/{slug} allows anonymous users to load collections")
    void getCollectionBySlug_anonymous_returnsCollection() throws Exception {
        when(contentService.getCollectionBySlug("featured-cameras"))
                .thenReturn(Optional.of(buildCollectionResponse()));

        mockMvc.perform(get("/api/content/collections/{slug}", "featured-cameras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("featured-cameras"))
                .andExpect(jsonPath("$.title").value("Featured Cameras"))
                .andExpect(jsonPath("$.items[0].title").value("Sony A7 IV"));
    }

    private HomepageResponse buildHomepageResponse() {
        HomepageResponse.HomepageModuleItemResponse item =
                HomepageResponse.HomepageModuleItemResponse.builder()
                        .imageUrl("/images/home/hero/trade-smarter.jpg")
                        .headline("Fresh Finds Every Day")
                        .subheadline("Browse newly listed devices.")
                        .linkType("route")
                        .linkValue("/listings")
                        .accentLabel("Browse")
                        .displayOrder(1)
                        .build();

        HomepageResponse.HomepageModuleResponse module =
                HomepageResponse.HomepageModuleResponse.builder()
                        .slug("home-hero")
                        .moduleType("hero")
                        .title("Trade Smarter")
                        .subtitle("Go straight to curated devices.")
                        .displayOrder(1)
                        .items(List.of(item))
                        .build();

        return HomepageResponse.builder()
                .modules(List.of(module))
                .build();
    }

    private CuratedCollectionResponse buildCollectionResponse() {
        ListingResponse listing = ListingResponse.builder()
                .id(7L)
                .title("Sony A7 IV")
                .price(new BigDecimal("1799.00"))
                .condition(Condition.LIKE_NEW)
                .status(ListingStatus.AVAILABLE)
                .city("Shanghai")
                .region("Shanghai")
                .primaryImageUrl("/uploads/listings/sony-a7-iv.jpg")
                .categoryId(4L)
                .categoryName("Cameras")
                .createdAt(LocalDateTime.of(2026, 3, 29, 10, 0))
                .build();

        return CuratedCollectionResponse.builder()
                .slug("featured-cameras")
                .title("Featured Cameras")
                .subtitle("Mirrorless picks")
                .description("Handpicked cameras.")
                .coverImageUrl("/images/home/collections/featured-cameras.jpg")
                .targetType("category")
                .targetValue("4")
                .displayOrder(1)
                .items(List.of(listing))
                .build();
    }
}
