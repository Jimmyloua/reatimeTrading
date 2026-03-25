---
phase: 06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization
plan: 01
subsystem: infra
tags: [redis, websocket, stomp, presence, pubsub, backend]
requires:
  - phase: 06-00
    provides: backend red tests for Redis presence and cross-node fan-out
provides:
  - Redis pub/sub fan-out for websocket message and presence delivery
  - Redis-backed ephemeral presence sessions with timeout-based offline transitions
  - Presence payloads with updatedAt timestamps for reconnect-safe UI preservation
affects: [06-02, chat, realtime, presence]
tech-stack:
  added: [Redis pub/sub listener container, Redis-backed presence session tracking]
  patterns: [persistence-first MySQL writes then Redis fan-out, Redis session TTL plus per-user session set]
key-files:
  created:
    - backend/src/main/java/com/tradingplatform/config/RedisPubSubConfig.java
    - backend/src/main/java/com/tradingplatform/chat/redis/ChatRealtimeEvent.java
    - backend/src/main/java/com/tradingplatform/chat/redis/RedisChannels.java
    - backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventPublisher.java
    - backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventSubscriber.java
  modified:
    - backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java
    - backend/src/main/java/com/tradingplatform/chat/service/PresenceService.java
    - backend/src/main/java/com/tradingplatform/chat/service/PresenceSessionMaintenance.java
    - backend/src/main/java/com/tradingplatform/chat/dto/PresenceUpdateResponse.java
    - backend/src/test/java/com/tradingplatform/chat/controller/ChatWebSocketControllerTest.java
    - backend/src/test/java/com/tradingplatform/chat/integration/RedisChatFanoutIntegrationTest.java
    - backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java
key-decisions:
  - "Redis handles only ephemeral presence and websocket fan-out; MySQL remains the durable message source of truth."
  - "Presence sessions are stored as Redis TTL keys plus per-user session sets so multi-session disconnects do not mark a seller offline prematurely."
  - "PresenceServiceRedisTest starts a disposable local redis-server process because Docker/Testcontainers is unavailable in this environment."
patterns-established:
  - "Publish realtime events after chat persistence, then relay locally on every node through Redis subscriptions."
  - "Track presence with session TTLs and preserve last-seen timestamps separately from online/offline state."
requirements-completed: [P6-01, P6-05]
duration: 17min
completed: 2026-03-25
---

# Phase 6 Plan 01: Redis Presence and Cross-Node Fan-Out Backend Summary

**Redis pub/sub websocket fan-out with Redis-backed session presence, timeout cleanup, and reconnect-safe presence timestamps.**

## Performance

- **Duration:** 17 min
- **Started:** 2026-03-25T01:19:00Z
- **Completed:** 2026-03-25T01:36:00Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments

- Added dedicated Redis realtime channels plus publisher/subscriber wiring for message delivery and presence fan-out across app nodes.
- Replaced in-memory presence maps with Redis session TTL tracking and per-user session membership, preserving last seen data after timeout or reconnect churn.
- Extended presence payloads with `updatedAt` and moved timeout-driven offline broadcasts onto the same Redis event path as connect/disconnect transitions.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Redis pub/sub contracts for message and presence fan-out per P6-05** - `4e2c4cd0` (test), `76076744` (feat)
2. **Task 2: Move presence tracking to Redis-backed ephemeral state and publish transitions per P6-01** - `eaff4e7f` (test), `1a83dfcc` (feat)

## Files Created/Modified

- `backend/src/main/java/com/tradingplatform/config/RedisPubSubConfig.java` - subscribes every node to Redis message and presence channels
- `backend/src/main/java/com/tradingplatform/chat/redis/ChatRealtimeEvent.java` - shared event envelope for message delivery and presence updates
- `backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventPublisher.java` - serializes and publishes transient realtime events
- `backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventSubscriber.java` - forwards Redis events to `/queue/messages` and `/topic/presence.{userId}`
- `backend/src/main/java/com/tradingplatform/chat/service/PresenceService.java` - Redis-backed presence ownership with TTL cleanup and last-seen retention
- `backend/src/main/java/com/tradingplatform/chat/service/PresenceSessionMaintenance.java` - publishes timeout-driven offline transitions through Redis
- `backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java` - persists messages first, then publishes Redis fan-out events and updated presence payloads
- `backend/src/main/java/com/tradingplatform/chat/dto/PresenceUpdateResponse.java` - adds `updatedAt` for reconnect-safe frontend presence handling
- `backend/src/test/java/com/tradingplatform/chat/integration/RedisChatFanoutIntegrationTest.java` - locks Redis channel and subscriber delivery contracts
- `backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java` - verifies Redis session keys, TTL refresh, timeout expiry, and last-seen retention

## Decisions Made

- Redis pub/sub remains transient infrastructure only; message durability and ordering stay in MySQL.
- Presence truth is keyed by user session membership in Redis, not process-local memory.
- The Redis presence test boots a disposable local Redis process so verification does not depend on Docker.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Maven was using Java 8 instead of the project JDK**
- **Found during:** Task 1 verification
- **Issue:** Surefire could not run JDK 21-compiled tests because `JAVA_HOME` pointed at Java 8.
- **Fix:** Ran verification commands with `JAVA_HOME=D:\Java\JDK21`.
- **Files modified:** None
- **Verification:** `mvn -q "-Dtest=RedisChatFanoutIntegrationTest,PresenceServiceRedisTest" test`
- **Committed in:** not applicable (environment-only fix)

**2. [Rule 3 - Blocking] Docker was unavailable for Redis-backed presence tests**
- **Found during:** Task 2 TDD red step
- **Issue:** Testcontainers could not start because no Docker environment was available.
- **Fix:** Reworked `PresenceServiceRedisTest` to launch a disposable local `redis-server.exe` process on a free port.
- **Files modified:** backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java
- **Verification:** `mvn -q "-Dtest=PresenceServiceRedisTest" test`
- **Committed in:** `1a83dfcc` (part of task commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes were environment-related and necessary to complete verification without changing plan scope.

## Issues Encountered

- The worktree already had unrelated `.planning`, frontend build, and upload changes. They were left untouched.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Backend multi-node presence and realtime fan-out contracts are in place for the frontend seller-sync and responsive messages work in `06-02`.
- Residual test noise from Lettuce reconnect logs occurs when the disposable Redis process exits, but the dedicated suites pass.

## Self-Check: PASSED

- Summary file exists.
- Key implementation files exist.
- Task commits `4e2c4cd0`, `76076744`, `eaff4e7f`, and `1a83dfcc` are present in git history.

---
*Phase: 06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization*
*Completed: 2026-03-25*
