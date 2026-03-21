# Phase 2: Core Marketplace (Listings and Discovery) - Research

**Researched:** 2026-03-21
**Domain:** E-commerce Listings, Full-Text Search, Spatial Queries, Category Taxonomy
**Confidence:** HIGH

## Summary

This phase implements the core marketplace functionality: listing creation with multiple images, hierarchical category selection, item discovery through browsing and search, and location-based filtering. The implementation builds on Phase 1 authentication patterns and introduces MySQL full-text search for DISC-02, spatial queries for DISC-05 (location/distance filtering), and adjacency list model for hierarchical categories (LIST-03).

**Primary recommendation:** Implement Listing entity with relationships to User, Category, and ListingImage entities. Use MySQL FULLTEXT indexes for search, POINT spatial type for location, and parent_id self-reference for category hierarchy. Reuse existing AvatarService patterns for image upload with support for multiple images and primary selection.

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| LIST-01 | User can create item listing with title, description, and price | Listing entity, ListingService, CreateListingRequest DTO, validation |
| LIST-02 | User can upload multiple photos for a listing with primary image selection | ListingImage entity, ImageUploadService (extend AvatarService pattern), primary flag |
| LIST-03 | User can select category for item (hierarchical electronics categories) | Category entity with parent_id, adjacency list model, seed data for electronics |
| LIST-04 | User can specify item condition (new, like new, good, fair, poor) | Condition enum, @Enumerated in Listing entity, filter support |
| LIST-05 | User can specify item location (city/region for local pickup) | Location fields (city, region, latitude, longitude), POINT spatial type optional |
| LIST-06 | User can edit their own listings | UpdateListingRequest, ownership validation, partial update support |
| LIST-07 | User can delete their own listings | Soft delete vs hard delete, ownership validation, cascade images |
| LIST-08 | User can mark items as sold, available, or reserved | ListingStatus enum, status transition rules, update endpoint |
| DISC-01 | User can browse items by category | Category query, findAllByCategoryId including children (recursive CTE or multiple queries) |
| DISC-02 | User can search items by full-text search (title, description) | MySQL FULLTEXT index, MATCH AGAINST query, @Query native |
| DISC-03 | User can filter items by price range | Specification pattern, minPrice/maxPrice parameters |
| DISC-04 | User can filter items by condition | Specification pattern, condition parameter |
| DISC-05 | User can filter items by location/distance | Spatial query ST_Distance_Sphere or lat/lng calculation, distance parameter |
| DISC-06 | User can view item detail page with all listing information | ListingDetailResponse DTO, join fetch images, category, seller |
| DISC-07 | User can view seller information on item detail page | Embed UserProfileResponse in ListingDetailResponse |

</phase_requirements>

## Standard Stack

### Core Backend (Reuse from Phase 1)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4.x | Backend framework | Already in pom.xml |
| Spring Data JPA | Included | ORM and data access | Listing, Category, ListingImage entities |
| MapStruct | 1.6.3 | DTO mapping | Listing to ListingResponse mapping |
| Liquibase | Included | Database migrations | Schema for listings, categories, images |
| MySQL Connector | 8.x | Database driver | Already in pom.xml |
| Testcontainers | 1.20.4 | Integration testing | Already in pom.xml |

### Core Frontend (Reuse from Phase 1)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.2.x | UI framework | Already in package.json |
| TanStack Query | 5.91.x | Server state management | Listing queries, infinite scroll |
| React Hook Form | 7.71.x | Form handling | Create/edit listing forms |
| Zod | 4.3.x | Schema validation | Listing form validation |
| React Dropzone | 15.0.x | File uploads | Multiple image uploads |
| Lucide React | 0.577.x | Icons | UI icons |

### Supporting Backend
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hibernate Spatial | 7.x (included) | Spatial data types | POINT type for location coordinates |
| Springdoc OpenAPI | 3.0.x | API documentation | Optional, for API documentation |

### Supporting Frontend
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| clsx + tailwind-merge | 2.x + 3.x | Conditional classes | Already in package.json |
| Framer Motion | 12.x | Animations | Optional, for listing card animations |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| MySQL FULLTEXT | Elasticsearch | Elasticsearch requires separate infrastructure, better for scale; FULLTEXT sufficient for v1 |
| MySQL spatial types | Lat/lng columns with application calculation | Spatial types have better index support; application calc simpler for basic use |
| Adjacency list for categories | Nested sets, closure table | Adjacency list simplest, adequate for shallow hierarchy; nested sets better for deep trees |
| Soft delete | Hard delete | Soft delete preserves audit trail; hard delete simpler, better for GDPR |

**Installation:**

Backend (already in pom.xml from Phase 1):
```xml
<!-- Spatial support is included in Hibernate for Spring Boot 3.x -->
<!-- No additional dependencies needed for MySQL spatial types -->
```

Frontend (already in package.json from Phase 1):
```json
{
  "dependencies": {
    "react-dropzone": "^15.0.0"
  }
}
```

**Version verification:** Versions match existing pom.xml and package.json from Phase 1.

## Architecture Patterns

### Recommended Project Structure

```
backend/
├── src/main/java/com/tradingplatform/
│   ├── listing/
│   │   ├── Listing.java                    # Listing entity
│   │   ├── ListingController.java          # CRUD and search endpoints
│   │   ├── ListingService.java             # Business logic
│   │   ├── ListingRepository.java          # JPA repository with custom queries
│   │   ├── ListingSpecification.java       # Dynamic filtering
│   │   ├── ListingImage.java               # Image entity
│   │   ├── ListingImageService.java        # Multiple image handling
│   │   ├── Category.java                   # Category entity (hierarchical)
│   │   ├── CategoryRepository.java         # Category queries
│   │   ├── Condition.java                  # Enum: NEW, LIKE_NEW, GOOD, FAIR, POOR
│   │   ├── ListingStatus.java              # Enum: AVAILABLE, RESERVED, SOLD
│   │   └── dto/
│   │       ├── CreateListingRequest.java
│   │       ├── UpdateListingRequest.java
│   │       ├── ListingResponse.java
│   │       ├── ListingDetailResponse.java
│   │       ├── ListingSearchRequest.java
│   │       └── ImageUploadResponse.java
│   └── exception/
│       └── ErrorCode.java                  # Add LISTING_NOT_FOUND, etc.
├── src/main/resources/
│   └── db/changelog/
│       ├── 002-create-categories-table.xml
│       ├── 003-create-listings-table.xml
│       └── 004-create-listing-images-table.xml
└── pom.xml

frontend/
├── src/
│   ├── pages/
│   │   ├── CreateListingPage.tsx
│   │   ├── EditListingPage.tsx
│   │   ├── ListingDetailPage.tsx
│   │   └── BrowseListingsPage.tsx
│   ├── components/
│   │   ├── ListingCard.tsx
│   │   ├── ListingForm.tsx
│   │   ├── ImageUploader.tsx
│   │   ├── CategorySelect.tsx
│   │   ├── ListingFilters.tsx
│   │   └── ListingGrid.tsx
│   ├── api/
│   │   └── listingApi.ts
│   └── types/
│       └── listing.ts
└── package.json
```

### Pattern 1: Listing Entity with Relationships

**What:** JPA entity representing a marketplace listing with foreign keys to User and Category, status enum, and spatial location.

**When to use:** Core data model for all listing operations.

**Example:**
```java
@Entity
@Table(name = "listings", indexes = {
    @Index(name = "idx_listings_user", columnList = "user_id"),
    @Index(name = "idx_listings_category", columnList = "category_id"),
    @Index(name = "idx_listings_status", columnList = "status"),
    @Index(name = "idx_listings_price", columnList = "price")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    @Size(min = 3, max = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Size(max = 5000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Condition condition = Condition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Location fields
    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // Optional: MySQL POINT type for spatial queries
    // @Column(columnDefinition = "POINT")
    // private Point location;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isPrimary DESC, displayOrder ASC")
    private List<ListingImage> images = new ArrayList<>();
}
```

### Pattern 2: Category Hierarchy with Adjacency List

**What:** Self-referencing Category entity with parent_id for hierarchical electronics categories.

**When to use:** Category selection (LIST-03), category browsing (DISC-01).

**Example:**
```java
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_categories_parent", columnList = "parent_id"),
    @Index(name = "idx_categories_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @OrderBy("name ASC")
    private List<Category> children = new ArrayList<>();

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Transient
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    @Transient
    public int getDepth() {
        int depth = 0;
        Category current = this.parent;
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }
}
```

**Electronics Category Seed Data:**
```
- Electronics (root)
  - Phones & Tablets
    - Smartphones
    - Tablets
    - Phone Accessories
  - Computers & Laptops
    - Laptops
    - Desktops
    - Monitors
    - Computer Accessories
  - Cameras & Photography
    - Digital Cameras
    - Lenses
    - Camera Accessories
  - Audio & Video
    - Headphones
    - Speakers
    - Microphones
  - Gaming
    - Consoles
    - Games
    - Gaming Accessories
  - Networking
    - Routers
    - Switches
    - Network Storage
```

### Pattern 3: Listing Image with Primary Selection

**What:** Entity for multiple images per listing with isPrimary flag and display order.

**When to use:** Image upload (LIST-02), listing detail display.

**Example:**
```java
@Entity
@Table(name = "listing_images", indexes = {
    @Index(name = "idx_images_listing", columnList = "listing_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
}
```

### Pattern 4: Full-Text Search with MySQL FULLTEXT

**What:** Native SQL query using MATCH AGAINST for full-text search on title and description.

**When to use:** Search functionality (DISC-02).

**Example:**
```java
// In ListingRepository.java
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    @Query(value = "SELECT * FROM listings l " +
           "WHERE MATCH(l.title, l.description) AGAINST(:query IN BOOLEAN MODE) " +
           "AND l.status = 'AVAILABLE' AND l.is_deleted = false " +
           "ORDER BY l.created_at DESC", nativeQuery = true)
    Page<Listing> searchByFullText(@Param("query") String query, Pageable pageable);
}

// Liquibase migration for FULLTEXT index
// In 003-create-listings-table.xml, add after createTable:
<changeSet id="003-add-fulltext-index" author="system">
    <sql>CREATE FULLTEXT INDEX idx_listings_fulltext ON listings(title, description)</sql>
</changeSet>
```

**Search Query Examples:**
```sql
-- Boolean mode (supports operators)
MATCH(title, description) AGAINST('+laptop -gaming' IN BOOLEAN MODE)
-- Natural language mode (default)
MATCH(title, description) AGAINST('laptop gaming')
-- Query expansion
MATCH(title, description) AGAINST('laptop' WITH QUERY EXPANSION)
```

### Pattern 5: Distance Filtering with Coordinates

**What:** Haversine formula or MySQL ST_Distance_Sphere for distance-based filtering.

**When to use:** Location/distance filter (DISC-05).

**Example (Haversine in JPA):**
```java
// In ListingRepository.java - using Haversine formula
@Query(value = "SELECT l.*, " +
       "(6371 * acos(cos(radians(:lat)) * cos(radians(l.latitude)) * " +
       "cos(radians(l.longitude) - radians(:lng)) + sin(radians(:lat)) * " +
       "sin(radians(l.latitude)))) AS distance " +
       "FROM listings l " +
       "WHERE l.status = 'AVAILABLE' AND l.is_deleted = false " +
       "HAVING distance < :radiusKm " +
       "ORDER BY distance", nativeQuery = true)
List<Listing> findByLocationWithinRadius(
    @Param("lat") double latitude,
    @Param("lng") double longitude,
    @Param("radiusKm") double radiusKm
);

// Alternative: Using MySQL 8 spatial functions
@Query(value = "SELECT l.* FROM listings l " +
       "WHERE l.status = 'AVAILABLE' AND l.is_deleted = false " +
       "AND ST_Distance_Sphere(POINT(l.longitude, l.latitude), POINT(:lng, :lat)) <= :radiusMeters",
       nativeQuery = true)
List<Listing> findByLocationWithinRadiusSpatial(
    @Param("lat") double latitude,
    @Param("lng") double longitude,
    @Param("radiusMeters") double radiusMeters
);
```

### Pattern 6: Dynamic Filtering with Specification

**What:** Spring Data JPA Specification for combining multiple filters dynamically.

**When to use:** Price range (DISC-03), condition (DISC-04), category (DISC-01), status filtering.

**Example:**
```java
// ListingSpecification.java
public class ListingSpecification {

    public static Specification<Listing> withFilters(ListingSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only available listings
            predicates.add(cb.equal(root.get("status"), ListingStatus.AVAILABLE));
            predicates.add(cb.equal(root.get("deleted"), false));

            // Category filter (includes children)
            if (request.getCategoryId() != null) {
                List<Long> categoryIds = getCategoryAndDescendants(request.getCategoryId());
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            // Price range
            if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            // Condition filter
            if (request.getCondition() != null) {
                predicates.add(root.get("condition").in(request.getCondition()));
            }

            // Location filter (city/region)
            if (request.getCity() != null && !request.getCity().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), request.getCity().toLowerCase()));
            }
            if (request.getRegion() != null && !request.getRegion().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("region")), request.getRegion().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

// In ListingService.java
public Page<Listing> searchListings(ListingSearchRequest request, Pageable pageable) {
    if (request.getQuery() != null && !request.getQuery().isBlank()) {
        // Full-text search takes precedence
        return listingRepository.searchByFullText(request.getQuery(), pageable);
    }
    // Otherwise use specification for filtered browsing
    return listingRepository.findAll(ListingSpecification.withFilters(request), pageable);
}
```

### Pattern 7: Multiple Image Upload Service

**What:** Service for handling multiple image uploads, extending the AvatarService pattern.

**When to use:** Listing image uploads (LIST-02).

**Example:**
```java
@Service
@RequiredArgsConstructor
public class ListingImageService {

    private final ListingImageRepository imageRepository;
    private final ListingRepository listingRepository;

    @Value("${listing.upload.dir:./uploads/listings}")
    private String uploadDir;

    @Value("${listing.max-images:10}")
    private int maxImages;

    @Value("${listing.max-size:10485760}") // 10 MB
    private long maxFileSize;

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp"
    );

    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;

    @Transactional
    public List<ListingImage> uploadImages(Long listingId, List<MultipartFile> files, Integer primaryIndex) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        long currentCount = imageRepository.countByListingId(listingId);
        if (currentCount + files.size() > maxImages) {
            throw new ApiException(ErrorCode.IMAGE_LIMIT_EXCEEDED,
                "Maximum " + maxImages + " images allowed per listing");
        }

        List<ListingImage> uploadedImages = new ArrayList<>();
        int order = (int) currentCount;

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            validateFile(file);

            String filename = generateFilename(listingId, order, file.getContentType());
            storeImage(file, filename);

            ListingImage image = ListingImage.builder()
                .listing(listing)
                .imagePath(filename)
                .primary(i == primaryIndex && currentCount == 0)
                .displayOrder(order++)
                .build();

            uploadedImages.add(imageRepository.save(image));
        }

        // Ensure one primary image
        ensurePrimaryImage(listingId);

        return uploadedImages;
    }

    private void ensurePrimaryImage(Long listingId) {
        List<ListingImage> images = imageRepository.findByListingIdOrderByDisplayOrder(listingId);
        if (!images.isEmpty() && images.stream().noneMatch(ListingImage::isPrimary)) {
            ListingImage first = images.get(0);
            first.setPrimary(true);
            imageRepository.save(first);
        }
    }

    public void setPrimaryImage(Long listingId, Long imageId) {
        List<ListingImage> images = imageRepository.findByListingId(listingId);
        images.forEach(img -> img.setPrimary(img.getId().equals(imageId)));
        imageRepository.saveAll(images);
    }

    // Similar validation and thumbnail generation as AvatarService
}
```

### Pattern 8: Category Tree Query

**What:** Efficient query for retrieving category with all descendants for filtering.

**When to use:** Category browsing (DISC-01).

**Example:**
```java
// Using recursive CTE in MySQL 8
@Query(value = "WITH RECURSIVE category_tree AS (" +
       "SELECT id FROM categories WHERE id = :categoryId " +
       "UNION ALL " +
       "SELECT c.id FROM categories c " +
       "JOIN category_tree ct ON c.parent_id = ct.id" +
       ") SELECT id FROM category_tree", nativeQuery = true)
List<Long> findAllDescendantIds(@Param("categoryId") Long categoryId);

// In Java for smaller trees (acceptable for limited categories)
public List<Long> getCategoryAndDescendants(Long categoryId) {
    List<Long> ids = new ArrayList<>();
    ids.add(categoryId);
    collectChildIds(categoryId, ids);
    return ids;
}

private void collectChildIds(Long parentId, List<Long> ids) {
    List<Category> children = categoryRepository.findByParentId(parentId);
    for (Category child : children) {
        ids.add(child.getId());
        collectChildIds(child.getId(), ids);
    }
}
```

### Anti-Patterns to Avoid

- **N+1 queries on listing images:** Use `@EntityGraph` or `JOIN FETCH` to load images with listings
- **Storing images in database BLOB:** Use filesystem or S3, store only paths
- **Not indexing frequently filtered columns:** Add indexes on status, category_id, price, created_at
- **Full table scan for search:** Use FULLTEXT index, never LIKE '%query%'
- **Hard delete listings:** Use soft delete to preserve referential integrity and allow recovery
- **Accepting user-provided filenames:** Always generate unique filenames to prevent path traversal
- **Not validating image dimensions:** Large images can cause memory issues; resize on upload
- **Missing ownership checks:** Always verify listing.user.id matches authenticated user for edit/delete

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Full-text search | LIKE '%query%' | MySQL FULLTEXT index | LIKE scans entire table; FULLTEXT uses inverted index |
| Distance calculation | Custom Haversine in Java | MySQL ST_Distance_Sphere or built-in functions | Database-level filtering is faster and index-friendly |
| Category hierarchy | Nested set model (complex) | Adjacency list with recursive CTE | Simpler for shallow hierarchies, MySQL 8 supports recursive CTEs |
| Image resizing | Custom Java2D code | Thumbnailator or imgscalr library | Handles edge cases, EXIF orientation, quality |
| Dynamic filtering | String concatenation for SQL | Spring Data JPA Specification | Type-safe, SQL injection prevention, composable |
| Pagination | Custom limit/offset | Spring Data Pageable | Handles count queries, page metadata, sorting |

**Key insight:** MySQL 8 provides native full-text search and spatial functions that are sufficient for v1. Elasticsearch and dedicated search services add operational complexity without clear benefit until scale demands it.

## Common Pitfalls

### Pitfall 1: Missing FULLTEXT Index

**What goes wrong:** Search queries with LIKE '%keyword%' perform full table scans, becoming slow as listings grow.

**Why it happens:** Developers default to LIKE patterns from experience with small datasets.

**How to avoid:** Create FULLTEXT index on title and description columns. Use MATCH AGAINST in native queries. Boolean mode provides precise control over search operators.

**Warning signs:** Search response time increases linearly with listing count; database CPU spikes during searches.

### Pitfall 2: N+1 Query on Listing Images

**What goes wrong:** Loading a page of listings executes N+1 queries - one for listings, N for each listing's images.

**Why it happens:** JPA lazy-loads @OneToMany relationships by default, triggering queries on access.

**How to avoid:** Use `@EntityGraph(attributePaths = {"images", "category", "user"})` or explicit `JOIN FETCH` in queries. Consider DTO projection for list views.

**Warning signs:** Slow page loads on listing browse; Hibernate shows multiple SELECT statements in logs.

### Pitfall 3: Image Upload Path Traversal

**What goes wrong:** Attacker uploads file with name `../../../etc/passwd` or similar, potentially overwriting system files.

**Why it happens:** Using user-provided filename directly in file path construction.

**How to avoid:** Always generate filenames server-side (e.g., `listing_{id}_{order}.jpg`). Validate the final path is within the upload directory. Never trust user input for paths.

**Warning signs:** Filenames contain `..`, `/`, `\`, or null bytes.

### Pitfall 4: Missing Ownership Validation

**What goes wrong:** Any user can edit or delete any listing by manipulating the listing ID in requests.

**Why it happens:** Controllers check authentication but not resource ownership.

**How to avoid:** Add ownership check in service layer: `if (!listing.getUser().getId().equals(userId)) throw ApiException(LISTING_NOT_FOUND)`.

**Warning signs:** Users report missing listings; audit logs show edits from unexpected users.

### Pitfall 5: Large Image Upload Exhausting Memory

**What goes wrong:** Uploading large images causes OutOfMemoryError or slow response times.

**Why it happens:** Spring loads entire MultipartFile into memory; image processing libraries may hold multiple copies.

**How to avoid:** Set `spring.servlet.multipart.max-file-size=10MB` and `max-request-size=50MB` (for multiple images). Resize images immediately after upload. Consider streaming for future S3 migration.

**Warning signs:** Server crashes during concurrent uploads; memory usage spikes during listing creation.

### Pitfall 6: Price Precision Issues

**What goes wrong:** Prices display incorrectly (e.g., 9.99 becomes 9.99000001) or rounding errors accumulate.

**Why it happens:** Using Float or Double for monetary values introduces floating-point precision errors.

**How to avoid:** Always use `BigDecimal` for price fields. Use `@Column(precision = 10, scale = 2)` for database. Avoid float/double anywhere in price calculations.

**Warning signs:** Prices display with many decimal places; search filters miss items at exact price boundaries.

## Code Examples

### Liquibase Migration for Listings

```xml
<!-- 002-create-categories-table.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="002-create-categories-table" author="system">
        <createTable tableName="categories">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="slug" type="VARCHAR(100)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="description" type="VARCHAR(500)"/>
            <column name="parent_id" type="BIGINT"/>
            <column name="display_order" type="INT" defaultValueNumeric="0"/>
        </createTable>

        <createIndex tableName="categories" indexName="idx_categories_parent">
            <column name="parent_id"/>
        </createIndex>

        <addForeignKeyConstraint baseTableName="categories" baseColumnNames="parent_id"
                                 constraintName="fk_categories_parent"
                                 referencedTableName="categories" referencedColumnNames="id"/>
    </changeSet>
</databaseChangeLog>

<!-- 003-create-listings-table.xml -->
<databaseChangeLog ...>
    <changeSet id="003-create-listings-table" author="system">
        <createTable tableName="listings">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="title" type="VARCHAR(200)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="price" type="DECIMAL(10, 2)">
                <constraints nullable="false"/>
            </column>
            <column name="condition" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="category_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="city" type="VARCHAR(100)"/>
            <column name="region" type="VARCHAR(100)"/>
            <column name="latitude" type="DOUBLE"/>
            <column name="longitude" type="DOUBLE"/>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP"/>
            <column name="is_deleted" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex tableName="listings" indexName="idx_listings_user">
            <column name="user_id"/>
        </createIndex>
        <createIndex tableName="listings" indexName="idx_listings_category">
            <column name="category_id"/>
        </createIndex>
        <createIndex tableName="listings" indexName="idx_listings_status">
            <column name="status"/>
        </createIndex>
        <createIndex tableName="listings" indexName="idx_listings_price">
            <column name="price"/>
        </createIndex>

        <addForeignKeyConstraint baseTableName="listings" baseColumnNames="user_id"
                                 constraintName="fk_listings_user"
                                 referencedTableName="users" referencedColumnNames="id"/>
        <addForeignKeyConstraint baseTableName="listings" baseColumnNames="category_id"
                                 constraintName="fk_listings_category"
                                 referencedTableName="categories" referencedColumnNames="id"/>
    </changeSet>

    <changeSet id="003-add-fulltext-index" author="system">
        <sql dbms="mysql">CREATE FULLTEXT INDEX idx_listings_fulltext ON listings(title, description)</sql>
    </changeSet>
</databaseChangeLog>

<!-- 004-create-listing-images-table.xml -->
<databaseChangeLog ...>
    <changeSet id="004-create-listing-images-table" author="system">
        <createTable tableName="listing_images">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="listing_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="image_path" type="VARCHAR(500)">
                <constraints nullable="false"/>
            </column>
            <column name="is_primary" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="display_order" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex tableName="listing_images" indexName="idx_images_listing">
            <column name="listing_id"/>
        </createIndex>

        <addForeignKeyConstraint baseTableName="listing_images" baseColumnNames="listing_id"
                                 constraintName="fk_images_listing"
                                 referencedTableName="listings" referencedColumnNames="id"
                                 onDelete="CASCADE"/>
    </changeSet>
</databaseChangeLog>
```

### Listing Controller with CRUD

```java
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final ListingImageService imageService;

    @PostMapping
    public ResponseEntity<ListingResponse> createListing(
        @Valid @RequestBody CreateListingRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        Listing listing = listingService.createListing(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.toResponse(listing));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<List<ImageUploadResponse>> uploadImages(
        @PathVariable Long id,
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam(value = "primaryIndex", defaultValue = "0") int primaryIndex,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        // Ownership check happens in service
        List<ListingImage> images = imageService.uploadImages(id, files, primaryIndex, principal.getId());
        return ResponseEntity.ok(images.stream()
            .map(img -> new ImageUploadResponse(img.getId(), img.getImagePath(), img.isPrimary()))
            .toList());
    }

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> searchListings(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) List<Condition> condition,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String region,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        ListingSearchRequest request = ListingSearchRequest.builder()
            .query(query)
            .categoryId(categoryId)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .condition(condition)
            .city(city)
            .region(region)
            .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSortString(sort)));
        Page<Listing> results = listingService.searchListings(request, pageable);
        return ResponseEntity.ok(results.map(listingService::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailResponse> getListingDetail(@PathVariable Long id) {
        Listing listing = listingService.getListingDetail(id);
        return ResponseEntity.ok(listingService.toDetailResponse(listing));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(
        @PathVariable Long id,
        @Valid @RequestBody UpdateListingRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        Listing listing = listingService.updateListing(id, request, principal.getId());
        return ResponseEntity.ok(listingService.toResponse(listing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(
        @PathVariable Long id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        listingService.deleteListing(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ListingResponse> updateStatus(
        @PathVariable Long id,
        @RequestBody UpdateStatusRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        Listing listing = listingService.updateStatus(id, request.getStatus(), principal.getId());
        return ResponseEntity.ok(listingService.toResponse(listing));
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| LIKE '%query%' for search | FULLTEXT index with MATCH AGAINST | MySQL 5.6+ | 10-100x faster search on large datasets |
| Lat/lng columns with app calculation | Spatial types (POINT) with ST_Distance_Sphere | MySQL 5.7+ | Index support for distance queries |
| Nested sets for hierarchies | Adjacency list with recursive CTEs | MySQL 8.0+ | Simpler CRUD, acceptable for shallow trees |
| Multiple image sizes pre-generated | Single image + on-demand resize | 2015+ | Storage efficiency, CDNs can resize |
| Hard delete | Soft delete with is_deleted flag | Industry standard | Audit trail, recovery possible |

**Deprecated/outdated:**
- **MyISAM for FULLTEXT:** InnoDB has supported FULLTEXT since MySQL 5.6. Use InnoDB for transactional consistency.
- **Elasticsearch for small datasets:** Adds operational overhead without benefit until 100K+ documents.
- **Separate search microservice for v1:** Premature optimization. Keep search in main service until scale demands separation.

## Open Questions

1. **Maximum Images Per Listing**
   - What we know: Multiple images are required (LIST-02).
   - What's unclear: What's the optimal limit? 5, 10, or more?
   - Recommendation: Start with 10 images maximum per listing. Most marketplaces use 8-12. Add configuration property for easy adjustment.

2. **Image Storage Strategy**
   - What we know: AvatarService uses local filesystem.
   - What's unclear: Should listings use the same approach, or prepare for S3 from the start?
   - Recommendation: Use local filesystem for v1 with same abstraction pattern as AvatarStorageService. Abstract storage behind interface (ImageStorageService) to allow S3 migration without code changes.

3. **Category Depth Limit**
   - What we know: Electronics categories typically have 2-3 levels.
   - What's unclear: Should we enforce a maximum depth?
   - Recommendation: Limit to 3 levels (Electronics > Phones & Tablets > Smartphones). Deeper hierarchies confuse users and complicate filtering.

4. **Location Data Source**
   - What we know: Users need to specify city/region for local pickup (LIST-05).
   - What's unclear: Should we use a geocoding API for coordinates, or let users enter city name only?
   - Recommendation: Accept city/region as text fields for v1. Store lat/lng optionally if user provides or we geocode later. Avoid external API dependencies for v1.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Testcontainers 1.20.4 (existing) |
| Config file | `src/test/resources/application-test.yml` (existing) |
| Quick run command | `cd backend && mvn test -Dtest="*Test" -DfailIfNoTests=false` |
| Full suite command | `cd backend && mvn verify` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| LIST-01 | User can create listing with valid data | integration | `mvn test -Dtest=ListingControllerTest#testCreateListing` | Wave 0 |
| LIST-01 | Invalid listing returns validation errors | integration | `mvn test -Dtest=ListingControllerTest#testCreateListingInvalid` | Wave 0 |
| LIST-02 | User can upload multiple images | integration | `mvn test -Dtest=ListingImageServiceTest#testUploadMultipleImages` | Wave 0 |
| LIST-02 | Primary image selection works | unit | `mvn test -Dtest=ListingImageServiceTest#testSetPrimaryImage` | Wave 0 |
| LIST-03 | Category hierarchy loads correctly | unit | `mvn test -Dtest=CategoryRepositoryTest#testCategoryHierarchy` | Wave 0 |
| LIST-04 | Condition enum values valid | unit | `mvn test -Dtest=ListingTest#testConditionEnum` | Wave 0 |
| LIST-05 | Location fields persist correctly | integration | `mvn test -Dtest=ListingControllerTest#testCreateListingWithLocation` | Wave 0 |
| LIST-06 | User can edit own listing | integration | `mvn test -Dtest=ListingControllerTest#testUpdateOwnListing` | Wave 0 |
| LIST-06 | User cannot edit others' listings | integration | `mvn test -Dtest=ListingControllerTest#testUpdateOtherListingForbidden` | Wave 0 |
| LIST-07 | User can delete own listing | integration | `mvn test -Dtest=ListingControllerTest#testDeleteOwnListing` | Wave 0 |
| LIST-08 | Status transitions work correctly | unit | `mvn test -Dtest=ListingServiceTest#testStatusTransition` | Wave 0 |
| DISC-01 | Browse by category returns listings | integration | `mvn test -Dtest=ListingRepositoryTest#testFindByCategory` | Wave 0 |
| DISC-02 | Full-text search returns relevant results | integration | `mvn test -Dtest=ListingRepositoryTest#testFullTextSearch` | Wave 0 |
| DISC-03 | Price range filter works | unit | `mvn test -Dtest=ListingSpecificationTest#testPriceRangeFilter` | Wave 0 |
| DISC-04 | Condition filter works | unit | `mvn test -Dtest=ListingSpecificationTest#testConditionFilter` | Wave 0 |
| DISC-05 | Location filter works | integration | `mvn test -Dtest=ListingRepositoryTest#testLocationFilter` | Wave 0 |
| DISC-06 | Listing detail includes all data | integration | `mvn test -Dtest=ListingControllerTest#testGetListingDetail` | Wave 0 |
| DISC-07 | Seller info included in listing detail | integration | `mvn test -Dtest=ListingControllerTest#testListingDetailIncludesSeller` | Wave 0 |

### Sampling Rate
- **Per task commit:** `cd backend && mvn test -Dtest="*Test"`
- **Per wave merge:** `cd backend && mvn verify`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/.../listing/ListingControllerTest.java` — LIST-01, LIST-06, LIST-07, DISC-06, DISC-07
- [ ] `src/test/java/.../listing/ListingServiceTest.java` — LIST-08, business logic unit tests
- [ ] `src/test/java/.../listing/ListingRepositoryTest.java` — DISC-01, DISC-02, DISC-05
- [ ] `src/test/java/.../listing/ListingSpecificationTest.java` — DISC-03, DISC-04
- [ ] `src/test/java/.../listing/ListingImageServiceTest.java` — LIST-02
- [ ] `src/test/java/.../listing/CategoryRepositoryTest.java` — LIST-03
- [ ] `src/test/resources/listing-test-data.sql` — Test data for categories and listings

## Sources

### Primary (HIGH confidence)
- MySQL 8.0 Reference Manual - FULLTEXT search syntax and limitations
- MySQL 8.0 Reference Manual - Spatial data types and ST_Distance_Sphere
- Spring Data JPA Documentation - Specification pattern, Pageable
- Existing Phase 1 codebase patterns - AvatarService, User entity, ErrorCode enum
- CLAUDE.md Project Instructions - Mandated stack versions

### Secondary (MEDIUM confidence)
- Baeldung articles on Spring Data JPA Specifications
- Baeldung articles on MySQL full-text search with Spring Boot
- Industry patterns for e-commerce listing data models

### Tertiary (LOW confidence)
- Web search results for distance calculation approaches
- Community discussions on category hierarchy implementations

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All versions from existing pom.xml/package.json, proven Phase 1 patterns
- Architecture: HIGH - Standard Spring Data JPA patterns, well-documented MySQL features
- Pitfalls: HIGH - Common issues in e-commerce implementations are well-documented

**Research date:** 2026-03-21
**Valid until:** 30 days - Patterns are stable, MySQL features are mature