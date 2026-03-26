package com.tradingplatform.content.service;

import com.tradingplatform.content.dto.CuratedCollectionResponse;
import com.tradingplatform.content.dto.HomepageResponse;
import com.tradingplatform.content.entity.CuratedCollection;
import com.tradingplatform.content.entity.CuratedCollectionItem;
import com.tradingplatform.content.entity.HomepageModule;
import com.tradingplatform.content.entity.HomepageModuleItem;
import com.tradingplatform.content.repository.CuratedCollectionRepository;
import com.tradingplatform.content.repository.HomepageModuleRepository;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.enums.ListingStatus;
import com.tradingplatform.listing.dto.ListingResponse;
import com.tradingplatform.listing.enums.Condition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final CuratedCollectionRepository curatedCollectionRepository;
    private final HomepageModuleRepository homepageModuleRepository;

    public List<HomepageModuleContent> getActiveHomepageModules() {
        return homepageModuleRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .sorted(Comparator.comparing(HomepageModule::getDisplayOrder))
                .map(this::toHomepageModuleContent)
                .toList();
    }

    public Optional<CuratedCollectionContent> getActiveCollectionBySlug(String slug) {
        return curatedCollectionRepository.findBySlugAndActiveTrue(slug)
                .map(this::toCuratedCollectionContent);
    }

    public HomepageResponse getHomepage() {
        return HomepageResponse.builder()
                .modules(getActiveHomepageModules().stream()
                        .map(this::toHomepageModuleResponse)
                        .toList())
                .build();
    }

    public Optional<CuratedCollectionResponse> getCollectionBySlug(String slug) {
        return getActiveCollectionBySlug(slug)
                .map(this::toCuratedCollectionResponse);
    }

    private HomepageModuleContent toHomepageModuleContent(HomepageModule module) {
        List<HomepageModuleItemContent> items = module.getItems().stream()
                .sorted(Comparator.comparing(HomepageModuleItem::getDisplayOrder))
                .map(item -> new HomepageModuleItemContent(
                        item.getImageUrl(),
                        item.getHeadline(),
                        item.getSubheadline(),
                        item.getLinkType(),
                        item.getLinkValue(),
                        item.getAccentLabel(),
                        item.getDisplayOrder()
                ))
                .toList();

        return new HomepageModuleContent(
                module.getSlug(),
                module.getModuleType(),
                module.getTitle(),
                module.getSubtitle(),
                module.getDisplayOrder(),
                items
        );
    }

    private HomepageResponse.HomepageModuleResponse toHomepageModuleResponse(HomepageModuleContent module) {
        return HomepageResponse.HomepageModuleResponse.builder()
                .slug(module.slug())
                .moduleType(module.moduleType())
                .title(module.title())
                .subtitle(module.subtitle())
                .displayOrder(module.displayOrder())
                .items(module.items().stream()
                        .map(item -> HomepageResponse.HomepageModuleItemResponse.builder()
                                .imageUrl(item.imageUrl())
                                .headline(item.headline())
                                .subheadline(item.subheadline())
                                .linkType(item.linkType())
                                .linkValue(item.linkValue())
                                .accentLabel(item.accentLabel())
                                .displayOrder(item.displayOrder())
                                .build())
                        .toList())
                .build();
    }

    private CuratedCollectionContent toCuratedCollectionContent(CuratedCollection collection) {
        List<CollectionListingCard> items = collection.getItems().stream()
                .sorted(Comparator.comparing(CuratedCollectionItem::getDisplayOrder))
                .map(item -> toCollectionListingCard(item, collection.getCoverImageUrl()))
                .flatMap(Optional::stream)
                .toList();

        return new CuratedCollectionContent(
                collection.getSlug(),
                collection.getTitle(),
                collection.getSubtitle(),
                collection.getDescription(),
                collection.getCoverImageUrl(),
                collection.getTargetType(),
                collection.getTargetValue(),
                collection.getDisplayOrder(),
                items
        );
    }

    private CuratedCollectionResponse toCuratedCollectionResponse(CuratedCollectionContent collection) {
        return CuratedCollectionResponse.builder()
                .slug(collection.slug())
                .title(collection.title())
                .subtitle(collection.subtitle())
                .description(collection.description())
                .coverImageUrl(collection.coverImageUrl())
                .targetType(collection.targetType())
                .targetValue(collection.targetValue())
                .displayOrder(collection.displayOrder())
                .items(collection.items().stream()
                        .map(this::toListingResponse)
                        .toList())
                .build();
    }

    private Optional<CollectionListingCard> toCollectionListingCard(CuratedCollectionItem item, String fallbackImageUrl) {
        Listing listing = item.getListing();
        if (listing == null || listing.getStatus() != ListingStatus.AVAILABLE || Boolean.TRUE.equals(listing.getDeleted())) {
            return Optional.empty();
        }

        Optional<String> primaryImageUrl = resolvePrimaryImageUrl(listing);
        String imageUrl = primaryImageUrl.orElse(fallbackImageUrl);
        if (imageUrl == null || imageUrl.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new CollectionListingCard(
                listing.getId(),
                listing.getTitle(),
                listing.getPrice(),
                listing.getCondition(),
                listing.getCity(),
                listing.getRegion(),
                imageUrl,
                listing.getCategory().getId(),
                listing.getCategory().getName(),
                listing.getCreatedAt(),
                item.getBadgeText(),
                primaryImageUrl.isEmpty()
        ));
    }

    private Optional<String> resolvePrimaryImageUrl(Listing listing) {
        return listing.getImages().stream()
                .filter(image -> image.getImagePath() != null && !image.getImagePath().isBlank())
                .sorted(Comparator
                        .comparing((ListingImage image) -> Boolean.TRUE.equals(image.getIsPrimary()))
                        .reversed()
                        .thenComparing(ListingImage::getDisplayOrder))
                .map(image -> "/uploads/listings/" + image.getImagePath())
                .findFirst();
    }

    private ListingResponse toListingResponse(CollectionListingCard card) {
        return ListingResponse.builder()
                .id(card.listingId())
                .title(card.title())
                .price(card.price())
                .condition(card.condition())
                .status(ListingStatus.AVAILABLE)
                .city(card.city())
                .region(card.region())
                .primaryImageUrl(card.imageUrl())
                .categoryId(card.categoryId())
                .categoryName(card.categoryName())
                .createdAt(card.createdAt())
                .build();
    }

    public record HomepageModuleContent(
            String slug,
            String moduleType,
            String title,
            String subtitle,
            Integer displayOrder,
            List<HomepageModuleItemContent> items
    ) {
    }

    public record HomepageModuleItemContent(
            String imageUrl,
            String headline,
            String subheadline,
            String linkType,
            String linkValue,
            String accentLabel,
            Integer displayOrder
    ) {
    }

    public record CuratedCollectionContent(
            String slug,
            String title,
            String subtitle,
            String description,
            String coverImageUrl,
            String targetType,
            String targetValue,
            Integer displayOrder,
            List<CollectionListingCard> items
    ) {
    }

    public record CollectionListingCard(
            Long listingId,
            String title,
            BigDecimal price,
            Condition condition,
            String city,
            String region,
            String imageUrl,
            Long categoryId,
            String categoryName,
            java.time.LocalDateTime createdAt,
            String badgeText,
            boolean usesFallbackImage
    ) {
    }
}
