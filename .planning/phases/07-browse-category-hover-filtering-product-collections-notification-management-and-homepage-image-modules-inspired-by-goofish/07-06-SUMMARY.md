---
phase: 07-browse-category-hover-filtering-product-collections-notification-management-and-homepage-image-modules-inspired-by-goofish
plan: 06
subsystem: api
tags: [spring-boot, react, dto, content, homepage, collections]
requires:
  - phase: 07-01
    provides: content schema and foundation service
provides:
  - backend homepage and collection endpoints
  - DTO mapping for ordered homepage modules and curated collection cards
  - frontend content API client and shared content payload types
affects: [07-03, homepage, browse, content-api]
tech-stack:
  added: []
  patterns: [backend DTO projection, route-safe content contracts, frontend contract mirroring]
key-files:
  created:
    - backend/src/main/java/com/tradingplatform/content/controller/ContentController.java
    - backend/src/main/java/com/tradingplatform/content/dto/CuratedCollectionResponse.java
    - backend/src/main/java/com/tradingplatform/content/dto/HomepageResponse.java
    - frontend/src/api/contentApi.ts
    - frontend/src/types/content.ts
  modified:
    - backend/src/main/java/com/tradingplatform/content/service/ContentService.java
key-decisions:
  - "Mapped the existing foundation service into explicit DTOs instead of leaking entity-oriented records to the controller layer."
  - "Mirrored backend module and link types directly in `frontend/src/types/content.ts` so homepage and browse UI can import one stable contract."
patterns-established:
  - "Content APIs live under `/api/content` with `/homepage` and `/collections/{slug}` as read-only endpoints."
  - "Curated collection cards reuse listing-summary semantics instead of inventing a second card payload."
requirements-completed: [P7-03]
completed: 2026-03-26
---

# Phase 07 Plan 06: Content API Delivery Contracts Summary

**The content foundation is now exposed as a stable backend/frontend contract for homepage modules and curated collections.**

## Accomplishments

- Added backend DTOs and a `ContentController` for `/api/content/homepage` and `/api/content/collections/{slug}`.
- Extended `ContentService` with API-facing mapping while preserving ordered active content behavior.
- Added frontend `contentApi` and `content` types so later UI work can consume one shared contract.

## Task Commits

1. **Task 1: Add DTO mapping and content controller endpoints on top of the foundation service** - `f8d3e400` (`feat`)
2. **Task 2: Create the frontend content client and type contracts for later UI plans** - `1fad71f0` (`feat`)

## Verification

- `backend`: `mvn -Dtest=ContentServiceTest,ContentControllerTest test`
- `frontend`: `npx tsc --noEmit`

## Self-Check: PASSED
