---
phase: 02-core-marketplace-listings-and-discovery
plan: 03
subsystem: listing-search
tags: [search, filtering, specification, full-text, categories, pagination]
dependency_graph:
  requires: [02-02]
  provides: [search-endpoint, category-hierarchy, specification-filtering]
  affects: []
tech_stack:
  added:
    - JPA Specification for dynamic filtering
    - MySQL FULLTEXT search for text queries
  patterns:
    - Specification pattern for composable filters
    - Repository pattern with SpecificationExecutor
key_files:
  created:
    - backend/src/main/java/com/tradingplatform/listing/dto/ListingSearchRequest.java
    - backend/src/main/java/com/tradingplatform/listing/specification/ListingSpecification.java
    - backend/src/main/resources/db/changelog/005-seed-categories.xml
  modified:
    - backend/src/main/java/com/tradingplatform/listing/service/ListingService.java
    - backend/src/main/java/com/tradingplatform/listing/controller/ListingController.java
    - backend/src/test/java/com/tradingplatform/listing/service/ListingSpecificationTest.java
    - backend/src/test/java/com/tradingplatform/listing/service/ListingSearchServiceTest.java
    - backend/src/main/resources/db/changelog/db.changelog-master.xml
decisions:
  - Use JPA Specification for dynamic filter composition
  - Use MySQL FULLTEXT index for text search with BOOLEAN MODE
  - Sanitize query input to prevent SQL injection in full-text search
  - Pre-fetch category descendants via recursive CTE for hierarchy filtering
metrics:
  duration: 15 minutes
  tasks_completed: 3
  tests_passed: 23
  files_changed: 7
  completed_date: 2026-03-21
---

# Phase 02 Plan 03: Search and Discovery Summary

## One-Liner

Implemented search and discovery functionality with JPA Specification dynamic filtering, MySQL full-text search, and hierarchical category browsing with seed data.

## Must-Haves Delivered

| Truth | Status |
|-------|--------|
| User can browse items by category including child categories | Delivered - Category hierarchy with recursive CTE descendant lookup |
| User can search items by title and description with full-text search | Delivered - MySQL FULLTEXT with BOOLEAN MODE |
| User can filter items by price range (minPrice, maxPrice) | Delivered - JPA Specification predicates |
| User can filter items by condition | Delivered - Multi-value condition filter |
| User can filter items by city or region | Delivered - Case-insensitive location filter |
| User can view item detail with seller information | Already in place from Plan 02 |
| Search results are paginated and sorted | Delivered - Pageable with custom sort parsing |

## What Was Built

### ListingSearchRequest DTO
- Filter fields: query, categoryId, minPrice, maxPrice, conditions, city, region
- Optional geo-location fields: latitude, longitude, radiusKm (for future distance search)
- Builder pattern for flexible construction

### ListingSpecification Utility Class
- `withFilters()` - Main method combining all filter predicates
- `isAvailable()` - Filters for AVAILABLE status and non-deleted
- `priceBetween()` - Price range filter
- `inCategories()` - Category ID list filter
- `inCity()` / `inRegion()` - Case-insensitive location filters
- Always applies status=AVAILABLE and deleted=false filters

### ListingService Enhancements
- `searchListings()` - Handles both full-text and specification-based search
- `sanitizeSearchQuery()` - Removes dangerous characters for BOOLEAN MODE
- `getCategoryTree()` - Returns root categories for browsing
- `getCategoryById()` - Single category lookup

### ListingController Endpoints
- `GET /api/listings` - Search with query parameters and pagination
- `GET /api/listings/categories` - Category tree for browsing
- `GET /api/listings/categories/{id}` - Single category by ID
- Sort parsing: "field,direction" format (e.g., "price,asc")

### Category Seed Data Migration
- 005-seed-categories.xml with 26 electronics categories
- 3-level hierarchy: Electronics (root) -> 6 subcategories -> 18 leaf categories
- Categories: Smartphones, Tablets, Laptops, Headphones, Cameras, Gaming, etc.

## Deviations from Plan

None - plan executed exactly as written.

## Key Decisions

1. **JPA Specification over custom query builder**: More maintainable, type-safe, and composable
2. **Pre-fetch category descendants**: Recursive CTE query returns all IDs upfront, avoiding N+1 queries
3. **Query sanitization**: Remove special characters that could cause issues in BOOLEAN MODE full-text search
4. **Case-insensitive location filter**: Better user experience when city names have varied capitalization

## Tests Summary

| Test Class | Tests | Status |
|------------|-------|--------|
| ListingSpecificationTest | 15 | Passed |
| ListingSearchServiceTest | 8 | Passed |
| **Total** | **23** | **All Passed** |

## Files Modified

```
backend/src/main/java/com/tradingplatform/listing/
  dto/ListingSearchRequest.java          [NEW]
  specification/ListingSpecification.java [NEW]
  service/ListingService.java            [MODIFIED]
  controller/ListingController.java      [MODIFIED]

backend/src/main/resources/db/changelog/
  005-seed-categories.xml                [NEW]
  db.changelog-master.xml                [MODIFIED]

backend/src/test/java/com/tradingplatform/listing/service/
  ListingSpecificationTest.java          [MODIFIED]
  ListingSearchServiceTest.java          [MODIFIED]
```

## Self-Check: PASSED

- [x] All created files exist
- [x] All commits exist in git history
- [x] Tests pass