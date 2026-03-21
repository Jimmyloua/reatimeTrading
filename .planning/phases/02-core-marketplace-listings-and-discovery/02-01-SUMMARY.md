---
phase: 02-core-marketplace-listings-and-discovery
plan: 01
subsystem: listing
tags: [database, schema, entity, enum, migration]
requires: [02-00]
provides:
  - Category entity with hierarchy
  - Listing entity with relationships
  - ListingImage entity with primary selection
  - Condition enum
  - ListingStatus enum
  - Database migrations for all tables
affects: [02-02, 02-03]
tech-stack:
  added:
    - Liquibase migrations 002, 003, 004
    - Spring Data JPA entities
    - JPA @EntityListeners for auditing
  patterns:
    - Self-referencing FK for category hierarchy
    - FULLTEXT index for search
    - CASCADE DELETE for listing images
key-files:
  created:
    - backend/src/main/java/com/tradingplatform/listing/entity/Category.java
    - backend/src/main/java/com/tradingplatform/listing/entity/Listing.java
    - backend/src/main/java/com/tradingplatform/listing/entity/ListingImage.java
    - backend/src/main/java/com/tradingplatform/listing/repository/CategoryRepository.java
    - backend/src/main/java/com/tradingplatform/listing/repository/ListingRepository.java
    - backend/src/main/java/com/tradingplatform/listing/enums/Condition.java
    - backend/src/main/java/com/tradingplatform/listing/enums/ListingStatus.java
    - backend/src/main/resources/db/changelog/002-create-categories-table.xml
    - backend/src/main/resources/db/changelog/003-create-listings-table.xml
    - backend/src/main/resources/db/changelog/004-create-listing-images-table.xml
  modified:
    - backend/src/main/resources/db/changelog/db.changelog-master.xml
    - backend/src/test/java/com/tradingplatform/listing/repository/CategoryRepositoryTest.java
    - backend/src/test/java/com/tradingplatform/listing/repository/ListingRepositoryTest.java
decisions:
  - Adjacency list pattern for category hierarchy (parent_id self-reference)
  - @OrderBy("name ASC") for consistent category children ordering
  - @OrderBy("isPrimary DESC, displayOrder ASC") for listing images
  - FULLTEXT index in separate changeSet with dbms="mysql" for portability
  - Using @CreatedDate/@LastModifiedDate with @EntityListeners instead of @PrePersist
metrics:
  duration: 20 minutes
  tasks: 3
  files: 13
  tests: 13 (8 CategoryRepository + 5 ListingRepository)
---

# Phase 02 Plan 01: Listing Database Schema Summary

**One-liner:** Created database schema for categories, listings, and listing images with enum types, proper indexes, and repository tests.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Category entity and Liquibase migration | 9decffed | Category.java, CategoryRepository.java, 002-create-categories-table.xml, CategoryRepositoryTest.java |
| 2 | Listing enums and Liquibase migration | 62962fce | Condition.java, ListingStatus.java, 003-create-listings-table.xml |
| 3 | ListingImage migration and ListingRepository tests | 297fe288 | Listing.java, ListingImage.java, ListingRepository.java, 004-create-listing-images-table.xml, ListingRepositoryTest.java |

## Implementation Details

### Category Entity
- Self-referencing `parent_id` for hierarchical categories
- `isLeaf()` method returns true if no children
- `getDepth()` method counts parent chain depth
- `@OrderBy("name ASC")` for consistent children ordering

### Listing Entity
- Uses `Condition` and `ListingStatus` enums
- `@OneToMany` with `CascadeType.ALL` and `orphanRemoval=true` for images
- Proper indexes on `user_id`, `category_id`, `status`, `price`
- Soft delete support with `is_deleted` column

### ListingImage Entity
- `is_primary` flag for primary image selection
- `display_order` for image ordering
- CASCADE DELETE on listing_id foreign key

### Database Migrations
- **002-create-categories-table.xml**: Categories with parent_id FK, slug unique index
- **003-create-listings-table.xml**: Listings with FKs to users and categories, FULLTEXT index
- **004-create-listing-images-table.xml**: Images with is_primary, display_order, CASCADE delete

## Deviations from Plan

None - plan executed exactly as written.

## Verification

```bash
export JAVA_HOME="/c/Program Files/Java/latest/jdk-21"
cd backend && mvn test -Dtest=CategoryRepositoryTest,ListingRepositoryTest
# Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```

## Self-Check: PASSED

- All entity files exist
- All migration files exist
- All repository files exist
- All tests pass (13 total)
- Commits verified: 9decffed, 62962fce, 297fe288

---

*Completed: 2026-03-21*
