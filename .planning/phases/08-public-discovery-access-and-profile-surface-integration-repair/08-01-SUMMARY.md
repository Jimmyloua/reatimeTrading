---
phase: 08-public-discovery-access-and-profile-surface-integration-repair
plan: 01
subsystem: api
tags: [spring-security, mockmvc, testing, discovery, ratings]
requires:
  - phase: 07-browse-category-hover-filtering-product-collections-notification-management-and-homepage-image-modules-inspired-by-goofish
    provides: anonymous homepage, browse, and public profile entry surfaces that depend on backend read access
provides:
  - anonymous GET authorization for public browse, content, and rating endpoints
  - MockMvc regression coverage for anonymous discovery and protected write routes
affects: [phase-08, public-discovery, profile-surface, backend-security]
tech-stack:
  added: []
  patterns: [method-scoped security matchers, controller-level auth regression tests]
key-files:
  created: [backend/src/test/java/com/tradingplatform/transaction/controller/RatingControllerTest.java]
  modified: [backend/src/main/java/com/tradingplatform/config/SecurityConfig.java, backend/src/test/java/com/tradingplatform/content/controller/ContentControllerTest.java, backend/src/test/java/com/tradingplatform/listing/controller/ListingControllerIT.java]
key-decisions:
  - "Kept anonymous access constrained to explicit GET matchers instead of broad path-wide permitAll rules."
  - "Used MockMvc controller tests to pin the discovery contract and kept the content test service-mocked to avoid unrelated collection fetch behavior."
patterns-established:
  - "Public backend discovery routes are opened only through HttpMethod.GET matchers in SecurityConfig."
  - "Anonymous-read tests must also assert unauthenticated write routes still return 401."
requirements-completed: [DISC-01, DISC-02, DISC-03, DISC-04, DISC-05, P7-01, P7-02, P7-03]
duration: 19min
completed: 2026-03-29
---

# Phase 08 Plan 01: Public API authorization alignment Summary

**Anonymous browse, content, and public rating reads are now pinned by MockMvc regression tests and explicit Spring Security GET matchers.**

## Performance

- **Duration:** 19 min
- **Started:** 2026-03-29T07:42:00Z
- **Completed:** 2026-03-29T08:01:38Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Replaced the placeholder content controller test with real MockMvc coverage for `/api/content/homepage` and `/api/content/collections/{slug}`.
- Added anonymous browse/category coverage in `ListingControllerIT` and new public rating coverage in `RatingControllerTest`.
- Narrowed Spring Security to explicit anonymous GET matchers for discovery and public ratings while keeping write paths authenticated.

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace placeholder auth coverage with real anonymous discovery read tests** - `a2bcae18` (test)
2. **Task 2: Align Spring Security with public discovery reads and authenticated writes** - `c767e020` (feat)

## Files Created/Modified
- `backend/src/main/java/com/tradingplatform/config/SecurityConfig.java` - explicit GET `permitAll` rules for browse, content, and public rating reads
- `backend/src/test/java/com/tradingplatform/content/controller/ContentControllerTest.java` - MockMvc content endpoint coverage with protected security filter chain still active
- `backend/src/test/java/com/tradingplatform/listing/controller/ListingControllerIT.java` - anonymous listing/category assertions plus protected write checks
- `backend/src/test/java/com/tradingplatform/transaction/controller/RatingControllerTest.java` - anonymous public rating reads and authenticated write assertions

## Decisions Made

- Used exact discovery-oriented GET matchers instead of broad `/api/listings/**` or `/api/ratings/**` public rules.
- Kept content endpoint verification controller-focused by mocking `ContentService`, which avoided unrelated collection fetch behavior while still exercising the real security chain.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Neutralized Redis listener startup in the targeted controller tests**
- **Found during:** Task 1
- **Issue:** Focused MockMvc tests failed to boot under `test` profile because `redisMessageListenerContainer` attempted a real Redis connection.
- **Fix:** Mocked `RedisMessageListenerContainer` in the targeted controller test classes so the auth suite could run without introducing app-level fallback behavior.
- **Files modified:** `backend/src/test/java/com/tradingplatform/content/controller/ContentControllerTest.java`, `backend/src/test/java/com/tradingplatform/listing/controller/ListingControllerIT.java`, `backend/src/test/java/com/tradingplatform/transaction/controller/RatingControllerTest.java`
- **Verification:** `mvn -q "-Dtest=ContentControllerTest,ListingControllerIT,RatingControllerTest" surefire:test`
- **Committed in:** `a2bcae18`

**2. [Rule 3 - Blocking] Removed unrelated persistence and H2 full-text failures from the new auth tests**
- **Found during:** Task 1
- **Issue:** Repository-backed collection test data triggered `MultipleBagFetchException`, and the anonymous browse query path hit H2's missing `MATCH()` function instead of the auth boundary.
- **Fix:** Switched the content contract to a mocked service response and changed the browse assertion to a plain paginated GET that still proves anonymous listing access.
- **Files modified:** `backend/src/test/java/com/tradingplatform/content/controller/ContentControllerTest.java`, `backend/src/test/java/com/tradingplatform/listing/controller/ListingControllerIT.java`
- **Verification:** `mvn -q "-Dtest=ContentControllerTest,ListingControllerIT,RatingControllerTest" surefire:test`
- **Committed in:** `a2bcae18`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes were necessary to verify the planned security work against the real controller boundary. No scope creep.

## Issues Encountered

- `mvn -q "-Dtest=ContentControllerTest,ListingControllerIT,RatingControllerTest" test` exceeded the repo's 60s backend test cap because Maven goal overhead pushed the run past the limit even after the suite was green.
- Used `mvn -q "-Dtest=ContentControllerTest,ListingControllerIT,RatingControllerTest" surefire:test` for final focused verification against the already-compiled targeted suite; it completed successfully in 45s.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Public browse, homepage content, and public rating read endpoints are now aligned with backend security.
- Phase 08-02 can build on these route guarantees to fix truthful profile listing counts without re-opening the authorization surface.

## Self-Check

PASSED

- FOUND: `.planning/phases/08-public-discovery-access-and-profile-surface-integration-repair/08-01-SUMMARY.md`
- FOUND: `a2bcae18`
- FOUND: `c767e020`

---
*Phase: 08-public-discovery-access-and-profile-surface-integration-repair*
*Completed: 2026-03-29*
