---
phase: 07-browse-category-hover-filtering-product-collections-notification-management-and-homepage-image-modules-inspired-by-goofish
plan: 02
subsystem: api
tags: [spring-boot, jpa, notifications, filtering, mockmvc, redis]
requires:
  - phase: 07-00
    provides: notification-management red tests and Phase 7 backend verification targets
provides:
  - filtered notification listing by tab and type
  - mark-visible-as-read backend contract for notification management
  - controller integration coverage for filtered notification APIs
affects: [07-04, 07-07, notification-center, backend-api]
tech-stack:
  added: []
  patterns: [optional read-state query branching, disposable local redis-backed controller integration tests]
key-files:
  created: []
  modified:
    - backend/src/main/java/com/tradingplatform/notification/repository/NotificationRepository.java
    - backend/src/main/java/com/tradingplatform/notification/service/NotificationService.java
    - backend/src/main/java/com/tradingplatform/notification/controller/NotificationController.java
    - backend/src/test/java/com/tradingplatform/notification/service/NotificationServiceTest.java
    - backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java
key-decisions:
  - "Split repository filtering into typed and untyped query/update methods so empty type filters never depend on a fragile empty IN clause."
  - "Reused the local disposable redis-server pattern in NotificationControllerTest so the controller contract can boot and verify under the existing Redis-backed test configuration."
patterns-established:
  - "Notification filters are expressed as tab=all|unread plus an optional NotificationType list and resolved in the service layer."
  - "Filtered bulk read actions operate only on unread rows currently visible under the same filter contract."
requirements-completed: [P7-05]
duration: 13min
completed: 2026-03-26
---

# Phase 07 Plan 02: Backend Notification Management Filters and Visible Read Actions Summary

**Filtered notification listing and mark-visible-as-read endpoints for unread/type-scoped notification management**

## Performance

- **Duration:** 13 min
- **Started:** 2026-03-26T01:08:00Z
- **Completed:** 2026-03-26T01:21:44Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Added unread/type-aware repository and service paths for `GET /api/notifications`.
- Added `PATCH /api/notifications/read-visible` so only the currently filtered visible unread set is marked read.
- Expanded backend tests to cover `tab=all`, `tab=unread`, typed filters, and controller-level filtered read behavior.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add filtered notification queries and mark-visible-as-read behavior** - `62e7822f` (feat)
2. **Task 2: Expose filtered notification-management endpoints in the controller** - `2f9c374c` (feat)

## Files Created/Modified

- `backend/src/main/java/com/tradingplatform/notification/repository/NotificationRepository.java` - Added filtered list and filtered bulk-read update queries.
- `backend/src/main/java/com/tradingplatform/notification/service/NotificationService.java` - Added `tab`/`types` filtering and `markVisibleAsRead`.
- `backend/src/main/java/com/tradingplatform/notification/controller/NotificationController.java` - Added filter params to list endpoint and exposed `/read-visible`.
- `backend/src/test/java/com/tradingplatform/notification/service/NotificationServiceTest.java` - Added service coverage for `all`, `unread`, typed filters, and visible-read behavior.
- `backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java` - Added integration coverage for filtered list/read-visible and local Redis bootstrapping for the controller context.

## Decisions Made

- Used separate repository methods for typed vs untyped filters so empty `types` never rely on provider-specific empty-collection `IN` behavior.
- Kept `markVisibleAsRead` constrained to unread rows even for `tab=all`, preserving history while matching notification-center semantics.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Bootstrapped Redis for controller integration tests**
- **Found during:** Task 2 (Expose filtered notification-management endpoints in the controller)
- **Issue:** `NotificationControllerTest` could not start because the test profile expects a live Redis listener container.
- **Fix:** Added the same disposable `redis-server` + `@DynamicPropertySource` pattern already used by the presence tests.
- **Files modified:** `backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java`
- **Verification:** `mvn -q "-Dtest=NotificationControllerTest,NotificationServiceTest" test` with JDK 21
- **Committed in:** `2f9c374c`

**2. [Rule 3 - Blocking] Repaired a Phase 7 controller test matcher compile error in the dirty worktree**
- **Found during:** Task 1 verification
- **Issue:** Existing `NotificationControllerTest` changes used an invalid Hamcrest `in(...)` matcher form, which blocked test compilation.
- **Fix:** Replaced it with `containsInAnyOrder(...)` while finishing the controller contract work.
- **Files modified:** `backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java`
- **Verification:** `mvn -q "-Dtest=NotificationControllerTest,NotificationServiceTest" test` with JDK 21
- **Committed in:** `2f9c374c`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes were required to verify the planned backend contract. No feature scope drift.

## Issues Encountered

- The shell defaulted Maven to `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_131`; verification was run with `C:\Program Files\Java\latest\jdk-21` to match the project runtime.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The frontend notification management page can now rely on canonical `tab` and `types` query params plus `/api/notifications/read-visible`.
- Remaining Phase 7 notification work can focus on frontend URL sync and dropdown/page unread-count parity rather than backend contract changes.

## Self-Check: PASSED

---
*Phase: 07-browse-category-hover-filtering-product-collections-notification-management-and-homepage-image-modules-inspired-by-goofish*
*Completed: 2026-03-26*
