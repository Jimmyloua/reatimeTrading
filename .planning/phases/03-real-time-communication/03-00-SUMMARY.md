---
gsd_state_version: 1.0
phase: 03-real-time-communication
plan: 00
subsystem: infrastructure
tags: [wave-0, test-stubs, dependencies, websocket, kafka, stomp]
requires: []
provides:
  - Test stub files for chat module (7 test classes)
  - Backend WebSocket/Kafka dependencies
  - Frontend STOMP client dependencies
affects:
  - backend/pom.xml
  - frontend/package.json
  - backend/src/test/java/com/tradingplatform/chat/
  - backend/src/test/java/com/tradingplatform/notification/
tech-stack:
  added:
    - spring-boot-starter-websocket
    - spring-kafka
    - "@stomp/stompjs@^7.3.0"
    - sockjs-client@^1.6.1
    - "@types/sockjs-client@^1.5.4"
  patterns:
    - Test stub pattern with @Disabled annotation
key-files:
  created:
    - backend/src/test/java/com/tradingplatform/chat/controller/ChatControllerTest.java
    - backend/src/test/java/com/tradingplatform/chat/controller/ChatWebSocketControllerTest.java
    - backend/src/test/java/com/tradingplatform/chat/service/ChatServiceTest.java
    - backend/src/test/java/com/tradingplatform/chat/repository/MessageRepositoryTest.java
    - backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java
    - backend/src/test/java/com/tradingplatform/notification/service/NotificationServiceTest.java
    - backend/src/test/java/com/tradingplatform/notification/repository/NotificationRepositoryTest.java
  modified:
    - backend/pom.xml
    - frontend/package.json
    - frontend/package-lock.json
decisions: []
metrics:
  duration: 5min
  tasks: 3
  files: 10
  started: "2026-03-21T17:18:06Z"
  completed: "2026-03-21T17:23:00Z"
---

# Phase 03 Plan 00: Wave 0 Test Infrastructure Summary

## One-Liner

Added WebSocket/Kafka dependencies to backend and STOMP client dependencies to frontend, plus created 7 test stub files for chat and notification modules to satisfy Nyquist compliance.

## What Was Done

### Task 1: Add WebSocket and Kafka dependencies to backend
- Added `spring-boot-starter-websocket` to backend/pom.xml
- Added `spring-kafka` to backend/pom.xml
- Placed after spring-session-data-redis dependency as specified

### Task 2: Add STOMP client dependencies to frontend
- Installed `@stomp/stompjs@^7.3.0` for STOMP protocol WebSocket client
- Installed `sockjs-client@^1.6.1` for WebSocket fallback transport
- Installed `@types/sockjs-client@^1.5.4` for TypeScript type definitions

### Task 3: Create test stub files for chat and notification modules
Created 7 test stub files with @Disabled placeholder tests:

**Chat Module:**
- `ChatControllerTest.java` - Tests for CHAT-01 (create conversation), CHAT-03 (get conversations)
- `ChatWebSocketControllerTest.java` - Tests for CHAT-02 (send/receive messages), CHAT-05 (typing indicators)
- `ChatServiceTest.java` - Tests for CHAT-02 (message persistence), CHAT-04 (conversation history)
- `MessageRepositoryTest.java` - Tests for CHAT-03 (find messages by conversation)

**Notification Module:**
- `NotificationControllerTest.java` - Tests for NOTF-03 (get notifications), NOTF-04 (mark as read)
- `NotificationServiceTest.java` - Tests for NOTF-01 (message notification), NOTF-02 (item sold notification), NOTF-04 (mark as read)
- `NotificationRepositoryTest.java` - Tests for NOTF-03 (find/count notifications)

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- Frontend dependencies verified: `npm ls @stomp/stompjs sockjs-client` shows both installed
- Backend pom.xml validated: XML syntax correct, dependencies in correct section
- All 7 test stub files exist with @Disabled annotations

## Commits

| Commit | Type | Description |
|--------|------|-------------|
| 83d3c564 | chore | Add WebSocket and Kafka dependencies to backend |
| 4db20a2d | chore | Add STOMP client dependencies to frontend |
| 7fdfc25c | test | Add test stubs for chat and notification modules |

## Next Steps

The following plans are now ready for implementation:
- 03-01-PLAN.md - Chat Entities and REST API (Wave 1)
- 03-02-PLAN.md - Notification Backend (Wave 1)

---

*Plan completed: 2026-03-21*