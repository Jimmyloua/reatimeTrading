---
phase: 08-public-discovery-access-and-profile-surface-integration-repair
plan: 02
subsystem: api
tags: [spring-boot, react-query, vitest, profiles, listings]
requires:
  - phase: 08-public-discovery-access-and-profile-surface-integration-repair
    provides: anonymous profile route access and public rating reads
provides:
  - listing-backed profile counts for self and public profile responses
  - anonymous-safe public profile handle rendering
  - focused backend and frontend profile regression coverage
affects: [phase-08, public-profile, profile-surface, ratings]
tech-stack:
  added: []
  patterns: [repository-backed profile aggregates, anonymous-safe public identity rendering]
key-files:
  created: [frontend/src/tests/user-profile-page.test.tsx]
  modified: [backend/src/main/java/com/tradingplatform/listing/repository/ListingRepository.java, backend/src/main/java/com/tradingplatform/user/UserController.java, backend/src/test/java/com/tradingplatform/controller/UserControllerTest.java, frontend/src/pages/UserProfilePage.tsx]
key-decisions:
  - "Derived listingCount directly from ListingRepository so the existing profile DTO contract stayed unchanged."
  - "Built the public profile handle from displayName with a user-id fallback because anonymous profile payloads intentionally omit email."
patterns-established:
  - "Profile response truthfulness comes from repository aggregation instead of controller stubs."
  - "Anonymous-facing identity strings must not depend on private fields such as email."
requirements-completed: [PROF-03, PROF-04]
duration: 18min
completed: 2026-03-29
---

# Phase 08 Plan 02: Truthful profile counts and anonymous-safe public identity Summary

**Profile APIs now return real listing totals, and the anonymous public profile route renders a stable handle plus ratings without login-only assumptions.**

## Performance

- **Duration:** 18 min
- **Started:** 2026-03-29T08:02:00Z
- **Completed:** 2026-03-29T08:20:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Replaced the remaining hardcoded `listingCount(0L)` profile stub with a repository-backed count of non-deleted seller listings.
- Extended backend profile tests to prove listing totals are non-zero when listings exist and exclude soft-deleted rows.
- Repaired the public profile identity line so anonymous viewers always see a deterministic handle and the ratings section stays visible.

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace hardcoded profile listing counts with listing-backed aggregation** - `cbb44071` (feat)
2. **Task 2: Repair the anonymous public profile surface and lock it with a frontend regression test** - `b50873f7` (test)

## Files Created/Modified
- `backend/src/main/java/com/tradingplatform/listing/repository/ListingRepository.java` - seller listing count query for non-deleted rows
- `backend/src/main/java/com/tradingplatform/user/UserController.java` - profile response builder now uses live listing totals
- `backend/src/test/java/com/tradingplatform/controller/UserControllerTest.java` - profile count coverage for self/public routes and soft-delete exclusion
- `frontend/src/pages/UserProfilePage.tsx` - anonymous-safe public handle generation with `@user-{id}` fallback
- `frontend/src/tests/user-profile-page.test.tsx` - logged-out profile route regression covering handle, listing count, and ratings visibility

## Decisions Made

- Kept the profile response shape unchanged and fixed truthfulness behind the controller boundary instead of widening the DTO.
- Treated the duplicated display name in the hero and profile card as intentional UI, so the regression test now targets semantic headings rather than a single text occurrence.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The new frontend test initially failed because `findByText('Camera Trader')` matched both the hero heading and the profile card heading once the page rendered successfully.
- Resolved by asserting against heading roles and levels, which matches the actual UI contract without weakening the page behavior.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Self and public profile routes now expose truthful listing totals from listing data.
- The anonymous public profile surface is stable and ready for the remaining Phase 08 integration repair work.

---
*Phase: 08-public-discovery-access-and-profile-surface-integration-repair*
*Completed: 2026-03-29*
