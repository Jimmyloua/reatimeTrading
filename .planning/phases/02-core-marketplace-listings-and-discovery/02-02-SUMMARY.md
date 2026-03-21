---
phase: 02-core-marketplace-listings-and-discovery
plan: 02
subsystem: listing
tags: [service, controller, dto, crud, rest-api, image-upload]
requires: [02-01]
provides:
  - ListingService with CRUD operations and ownership validation
  - ListingImageService with max 10 images limit
  - ListingStorageService with image resizing
  - ListingController with full REST API
  - DTOs for listing requests and responses
  - Integration tests for listing endpoints
affects: [02-03, 02-04, 02-05]
tech-stack:
  added:
    - Spring MVC REST Controller
    - MultipartFile image upload
    - BufferedImage image processing
    - Jakarta Validation annotations
  patterns:
    - Ownership validation in service layer
    - Soft delete for listings
    - DTO mapping in service
    - Public/Private endpoint security
key-files:
  created:
    - backend/src/main/java/com/tradingplatform/listing/controller/ListingController.java
    - backend/src/main/java/com/tradingplatform/listing/dto/CreateListingRequest.java
    - backend/src/main/java/com/tradingplatform/listing/dto/UpdateListingRequest.java
    - backend/src/main/java/com/tradingplatform/listing/dto/ListingResponse.java
    - backend/src/main/java/com/tradingplatform/listing/dto/ListingDetailResponse.java
    - backend/src/main/java/com/tradingplatform/listing/dto/ImageUploadResponse.java
    - backend/src/main/java/com/tradingplatform/listing/dto/UpdateStatusRequest.java
    - backend/src/test/java/com/tradingplatform/listing/dto/ListingDtoTest.java
    - backend/src/test/java/com/tradingplatform/listing/service/ListingServiceTest.java
    - backend/src/test/java/com/tradingplatform/listing/service/ListingImageServiceTest.java
    - backend/src/test/java/com/tradingplatform/listing/controller/ListingControllerIT.java
  modified:
    - backend/src/main/java/com/tradingplatform/listing/entity/Listing.java
    - backend/src/main/java/com/tradingplatform/listing/repository/ListingRepository.java
    - backend/src/main/java/com/tradingplatform/listing/repository/ListingImageRepository.java
    - backend/src/main/java/com/tradingplatform/listing/repository/CategoryRepository.java
    - backend/src/main/java/com/tradingplatform/listing/service/ListingService.java
    - backend/src/main/java/com/tradingplatform/listing/service/ListingImageService.java
    - backend/src/main/java/com/tradingplatform/listing/service/ListingStorageService.java
    - backend/src/main/java/com/tradingplatform/exception/ErrorCode.java
    - backend/src/main/java/com/tradingplatform/config/SecurityConfig.java
    - backend/src/test/java/com/tradingplatform/listing/repository/ListingRepositoryTest.java
decisions:
  - Added getSellerId() alias to Listing entity for ownership checks
  - Used findByIdAndDeletedFalse pattern for soft delete support
  - Made GET /api/listings/{id} and GET /api/listings/user/{id} public endpoints
  - Image upload validates file type, size (10MB max), and count (10 max)
  - First image uploaded is automatically set as primary
  - Image resizing to max 1200x1200 maintaining aspect ratio
metrics:
  duration: 30 minutes
  tasks: 4
  files: 21
  tests: 26 (8 ListingServiceTest + 9 ListingImageServiceTest + 9 ListingControllerIT)
---

# Phase 02 Plan 02: Listing CRUD Backend Summary

**One-liner:** Implemented listing CRUD backend with multiple image upload support, ownership validation, and full REST API endpoints.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create Listing and ListingImage entities with repositories | 4b2bcd57 | Listing.java, ListingRepository.java, ListingImageRepository.java, CategoryRepository.java, ListingRepositoryTest.java |
| 2 | Create DTOs and update ErrorCode enum | 3bb68594 | CreateListingRequest.java, UpdateListingRequest.java, ListingResponse.java, ListingDetailResponse.java, ImageUploadResponse.java, UpdateStatusRequest.java, ErrorCode.java, ListingDtoTest.java |
| 3 | Create ListingService and ListingImageService with tests | 2bea74da | ListingService.java, ListingImageService.java, ListingStorageService.java, ListingServiceTest.java, ListingImageServiceTest.java |
| 4 | Create ListingController with CRUD endpoints and integration tests | 6b520e09 | ListingController.java, ListingControllerIT.java, SecurityConfig.java |

## Implementation Details

### ListingService
- `createListing()` with category validation
- `updateListing()` with partial update support (only non-null fields)
- `deleteListing()` with soft delete (sets `deleted=true`)
- `updateStatus()` for AVAILABLE/RESERVED/SOLD transitions
- `getListingDetail()` with eager loading of images and category
- `toListingResponse()` and `toListingDetailResponse()` DTO mapping
- Ownership validation throws `LISTING_ACCESS_DENIED` for non-owners

### ListingImageService
- `uploadImages()` with max 10 images limit enforcement
- `setPrimaryImage()` resets all images then sets selected
- `deleteImage()` removes file and database record
- File validation: JPEG, PNG, WebP only, max 10MB
- First image automatically set as primary

### ListingStorageService
- Image resizing to max 1200x1200 maintaining aspect ratio
- Unique filename generation: `listing_{id}_{timestamp}_{order}.{ext}`
- Directory initialization on startup

### ListingController REST API
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/listings | Required | Create listing |
| GET | /api/listings/{id} | Public | Get listing details |
| PUT | /api/listings/{id} | Owner | Update listing |
| DELETE | /api/listings/{id} | Owner | Delete listing |
| PATCH | /api/listings/{id}/status | Owner | Update status |
| POST | /api/listings/{id}/images | Owner | Upload images |
| DELETE | /api/listings/{id}/images/{imageId} | Owner | Delete image |
| PATCH | /api/listings/{id}/images/{imageId}/primary | Owner | Set primary |
| GET | /api/listings/user/{userId} | Public | Get user listings |

### DTOs
- **CreateListingRequest**: Validation (title 3-200 chars, positive price, required fields)
- **UpdateListingRequest**: All fields optional for partial updates
- **ListingResponse**: Summary for list views
- **ListingDetailResponse**: Full details with nested `ImageInfo` and `SellerInfo`
- **ImageUploadResponse**: Upload result with URL and primary flag
- **UpdateStatusRequest**: Status update with validation

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed findBySellerId method in ListingService stub**
- **Found during:** Task 2 compilation
- **Issue:** ListingService called `findBySellerId()` which didn't exist in ListingRepository
- **Fix:** Changed to use `findByUserId()` method
- **Files modified:** ListingService.java
- **Commit:** 3bb68594

**2. [Rule 2 - Critical] Added public endpoint access for listing views**
- **Found during:** Task 4 integration tests
- **Issue:** GET /api/listings/{id} and GET /api/listings/user/{id} required authentication but should be public
- **Fix:** Updated SecurityConfig to allow public GET access to these endpoints
- **Files modified:** SecurityConfig.java
- **Commit:** 6b520e09

## Verification

```bash
export JAVA_HOME="/d/Java/JDK21"
cd backend && mvn test -Dtest=ListingServiceTest,ListingImageServiceTest,ListingControllerIT
# Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
```

## Self-Check: PASSED

- All service files exist
- All controller files exist
- All DTO files exist
- All tests pass (26 total)
- Commits verified: 4b2bcd57, 3bb68594, 2bea74da, 6b520e09

---

*Completed: 2026-03-21*