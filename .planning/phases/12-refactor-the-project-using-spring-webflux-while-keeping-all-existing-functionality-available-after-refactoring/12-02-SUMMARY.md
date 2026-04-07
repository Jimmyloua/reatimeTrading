---
phase: 12-refactor-the-project-using-spring-webflux-while-keeping-all-existing-functionality-available-after-refactoring
plan: 02
subsystem: http-slices
tags: [webflux, uploads, auth, listings, content, migration]
requires:
  - phase: 12-refactor-the-project-using-spring-webflux-while-keeping-all-existing-functionality-available-after-refactoring
    plan: 01
    provides: reactive foundation and security seam
provides:
  - reactive controller return types across auth, profile, listing, and content HTTP slices
  - staged reactive avatar upload controller using FilePart
  - explicit reactive filesystem wrappers for listing image storage
affects: [auth, user-profile, listings, content, uploads]
tech-stack:
  patterns: [bounded-elastic bridging, staged reactive replacement, explicit filesystem wrapping]
key-files:
  created:
    - backend/src/main/java/com/tradingplatform/avatar/AvatarLegacyController.java
  modified:
    - backend/src/main/java/com/tradingplatform/avatar/AvatarController.java
    - backend/src/main/java/com/tradingplatform/avatar/AvatarService.java
    - backend/src/main/java/com/tradingplatform/listing/controller/ListingController.java
    - backend/src/main/java/com/tradingplatform/listing/service/ListingImageService.java
    - backend/src/main/java/com/tradingplatform/listing/service/ListingStorageService.java
key-decisions:
  - "The reactive avatar upload path is staged behind a reactive-only controller while the servlet upload controller remains active until Plan 12-05 removes the legacy runtime."
  - "Blocking filesystem work is now exposed through explicit reactive wrappers instead of being silently mixed into request handling."
requirements-completed: [P12-03, P12-05]
completed: 2026-04-07
---

# Phase 12 Plan 02: Reactive HTTP Slice Summary

**The account, listing, content, and upload HTTP slice now has reactive controller seams in place, with the upload path explicitly staged for WebFlux rather than staying hidden behind servlet multipart assumptions.**

## Accomplishments

- Kept the auth, user, listing, and content endpoints on reactive `Mono` controller signatures while preserving their current route and DTO contracts.
- Converted `AvatarController` into the staged WebFlux upload controller using `FilePart` and moved the current servlet upload endpoint into `AvatarLegacyController` so the existing runtime keeps working until the final cutover.
- Refactored `AvatarService` so both servlet multipart uploads and reactive `FilePart` uploads flow through the same validated image-processing path.
- Added explicit reactive wrappers to `ListingStorageService` and switched listing image upload orchestration to `uploadImagesReactive(...)` so filesystem I/O is isolated behind clear bounded-elastic boundaries.

## Verification

- Frontend tests passed:
  - `cd frontend && npm test -- --run src/tests/create-listing.test.tsx src/tests/user-profile-page.test.tsx`
- Backend upload slice tests produced passing Surefire reports:
  - `AvatarControllerTest` - `Tests run: 8, Failures: 0, Errors: 0`
  - `ListingControllerIT` - `Tests run: 14, Failures: 0, Errors: 0`

## Issues Encountered

- The targeted backend Spring integration tests are still slower than the project feedback-budget target: `AvatarControllerTest` took 62.25s and `ListingControllerIT` took 69.63s in Surefire, so Maven invocations hit the 60-second command cap even though the reports were green.

## Next Phase Readiness

- Plan `12-03` can now replace the servlet STOMP transport with a WebFlux-compatible realtime boundary without the upload slice still depending on servlet multipart assumptions.
- Plan `12-05` still remains the real completion gate because the active production runtime is still mixed servlet + reactive infrastructure.
