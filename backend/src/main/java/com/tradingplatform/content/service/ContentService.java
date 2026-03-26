package com.tradingplatform.content.service;

import com.tradingplatform.content.entity.CuratedCollection;
import com.tradingplatform.content.entity.CuratedCollectionItem;
import com.tradingplatform.content.entity.HomepageModule;
import com.tradingplatform.content.entity.HomepageModuleItem;
import com.tradingplatform.content.repository.CuratedCollectionRepository;
import com.tradingplatform.content.repository.HomepageModuleRepository;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.entity.ListingImage;
import com.tradingplatform.listing.enums.ListingStatus;
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
                listing.getCity(),
                listing.getRegion(),
                imageUrl,
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
            String city,
            String region,
            String imageUrl,
            String badgeText,
            boolean usesFallbackImage
    ) {
    }
}
