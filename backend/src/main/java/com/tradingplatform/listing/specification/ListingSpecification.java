package com.tradingplatform.listing.specification;

import com.tradingplatform.listing.dto.ListingSearchRequest;
import com.tradingplatform.listing.entity.Listing;
import com.tradingplatform.listing.enums.ListingStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating JPA Specifications for Listing queries.
 * Provides dynamic filtering capabilities for listing search.
 */
public final class ListingSpecification {

    private ListingSpecification() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a Specification for filtering listings based on search request.
     *
     * @param request     the search request containing filter criteria
     * @param categoryIds pre-fetched list of category IDs (including descendants)
     * @return a Specification for the combined filters
     */
    public static Specification<Listing> withFilters(ListingSearchRequest request, List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter: status = AVAILABLE and deleted = false
            predicates.add(criteriaBuilder.equal(root.get("status"), ListingStatus.AVAILABLE));
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            // Category filter (using pre-fetched categoryIds including descendants)
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            // Price range filters
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            // Condition filter
            if (request.getConditions() != null && !request.getConditions().isEmpty()) {
                predicates.add(root.get("condition").in(request.getConditions()));
            }

            // Location filters (city/region - case insensitive)
            if (request.getCity() != null && !request.getCity().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("city")),
                        request.getCity().toLowerCase()
                ));
            }
            if (request.getRegion() != null && !request.getRegion().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("region")),
                        request.getRegion().toLowerCase()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a Specification that filters by status=AVAILABLE and deleted=false.
     *
     * @return a Specification for available, non-deleted listings
     */
    public static Specification<Listing> isAvailable() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("status"), ListingStatus.AVAILABLE),
                criteriaBuilder.isFalse(root.get("deleted"))
        );
    }

    /**
     * Creates a Specification for price range filter.
     *
     * @param minPrice minimum price (inclusive), null for no minimum
     * @param maxPrice maximum price (inclusive), null for no maximum
     * @return a Specification for the price range
     */
    public static Specification<Listing> priceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a Specification for category filter.
     *
     * @param categoryIds list of category IDs to filter by
     * @return a Specification for the category filter
     */
    public static Specification<Listing> inCategories(List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("category").get("id").in(categoryIds);
        };
    }

    /**
     * Creates a Specification for city filter (case-insensitive).
     *
     * @param city the city name to filter by
     * @return a Specification for the city filter
     */
    public static Specification<Listing> inCity(String city) {
        return (root, query, criteriaBuilder) -> {
            if (city == null || city.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.lower(root.get("city")), city.toLowerCase());
        };
    }

    /**
     * Creates a Specification for region filter (case-insensitive).
     *
     * @param region the region name to filter by
     * @return a Specification for the region filter
     */
    public static Specification<Listing> inRegion(String region) {
        return (root, query, criteriaBuilder) -> {
            if (region == null || region.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(criteriaBuilder.lower(root.get("region")), region.toLowerCase());
        };
    }
}