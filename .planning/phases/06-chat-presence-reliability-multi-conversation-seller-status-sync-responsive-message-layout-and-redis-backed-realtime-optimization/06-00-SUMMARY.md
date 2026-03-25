---
phase: 06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization
plan: 00
subsystem: testing
tags: [redis, websocket, presence, vitest, junit, responsive-layout]
requires:
  - phase: 05-notification-detail-actions-quick-settings-and-listing-chat-entry
    provides: Existing chat route, websocket delivery, notification deep links, and seller chat entry flows
provides:
  - Backend red tests for Redis-backed presence timeout and fan-out contracts
  - Frontend red tests for shared seller presence, duplicate-safe fallback, and responsive messages layout
  - Explicit Phase 6 Wave 0 failure points for later implementation plans
affects: [phase-06-plan-01, phase-06-plan-02, phase-06-plan-03, chat, websocket, redis]
tech-stack:
  added: []
  patterns: [tdd-red-tests, source-contract-tests, responsive-route-contracts]
key-files:
  created:
    - backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java
    - backend/src/test/java/com/tradingplatform/chat/integration/RedisChatFanoutIntegrationTest.java
    - frontend/src/tests/chat-presence-sync.test.tsx
    - frontend/src/tests/chat-realtime-fallback.test.tsx
    - frontend/src/tests/messages-responsive-layout.test.tsx
  modified: []
key-decisions:
  - "Wave 0 uses red tests to pin Redis channel names, presence stale-window behavior, duplicate-safe fallback behavior, and mobile shell expectations before implementation."
  - "Backend verification required a task-local JAVA_HOME override to JDK 21 because the workspace default JAVA_HOME still points to JDK 8."
patterns-established:
  - "Presence reliability must be expressed as explicit stale-window tests, not inferred from manual reconnect checks."
  - "Responsive /messages behavior is locked as route-level tests across desktop, tablet, and mobile modes."
requirements-completed: []  # Wave 0 red scaffolding only; implementation requirements remain open.
duration: 10min
completed: 2026-03-25
---

# Phase 6 Plan 00: Wave 0 Reliability and Responsive Chat Red Tests Summary

**Redis presence TTL, cross-node fan-out, shared seller presence, duplicate-safe fallback, and responsive `/messages` shell contracts captured as executable red tests**

## Performance

- **Duration:** 10 min
- **Started:** 2026-03-25T01:05:00Z
- **Completed:** 2026-03-25T01:15:40Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Added backend red tests for 60-second presence timeout handling, multi-session presence transitions, and Redis channel fan-out contracts.
- Added frontend red tests for per-seller presence sharing, stale-window copy, duplicate-safe realtime fallback, and `/messages` desktop/tablet/mobile layout rules.
- Verified both task batches fail on the intended gaps instead of passing implicitly.

## Task Commits

1. **Task 1: Add backend red tests for Redis presence and cross-node message fan-out** - `99371a26` (`test`)
2. **Task 2: Add frontend red tests for shared seller presence, fallback dedupe, and responsive layout** - `0db50094` (`test`)

## Files Created/Modified
- `backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java` - Red tests for multi-session presence, 60-second timeout, and retained last-seen expectations.
- `backend/src/test/java/com/tradingplatform/chat/integration/RedisChatFanoutIntegrationTest.java` - Red contract tests for `chat:realtime:message` and `chat:realtime:presence` wiring into websocket destinations.
- `frontend/src/tests/chat-presence-sync.test.tsx` - Red tests for shared seller presence subscription behavior and `Status updating` stale-window copy.
- `frontend/src/tests/chat-realtime-fallback.test.tsx` - Red tests for duplicate-safe fallback behavior, preview ordering, degraded helper copy, and no connected-mode polling.
- `frontend/src/tests/messages-responsive-layout.test.tsx` - Red tests for desktop/tablet two-pane mode and mobile single-pane/back-navigation contract.

## Decisions Made
- Used executable red tests instead of implementation changes, per the Wave 0 plan.
- Kept the fan-out backend contract explicit by asserting the literal Redis channels and websocket destinations that later plans must implement.

## Deviations from Plan

None - plan executed as written.

## Issues Encountered
- Backend Maven verification initially failed before reaching the red assertions because `mvn` inherited `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_131`. Re-running the task command with `JAVA_HOME=D:\Java\JDK21` produced the intended red test failures.
- The initial frontend fallback tests used fake-timer/waitFor combinations that timed out instead of failing on the real contract gaps. The tests were tightened so they now fail directly on duplicate handling, polling, and layout behavior.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 6 Plan 01 can now implement Redis-backed presence and cross-node fan-out against explicit backend red tests.
- Phase 6 Plans 02-03 can implement the seller-level presence store, duplicate-safe fallback behavior, and responsive route shell against explicit frontend red tests.

## Self-Check: PASSED

- Verified summary file exists.
- Verified all five Wave 0 red-test files exist.
- Verified task commits `99371a26` and `0db50094` exist in git history.

---
*Phase: 06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization*
*Completed: 2026-03-25*
