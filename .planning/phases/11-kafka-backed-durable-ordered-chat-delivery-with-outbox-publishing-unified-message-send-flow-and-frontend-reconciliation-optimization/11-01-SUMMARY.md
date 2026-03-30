---
phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
plan: 01
subsystem: api
tags: [chat, websocket, outbox, liquibase, jpa]
requires:
  - phase: 10-milestone-validation-and-audit-hygiene
    provides: validated baseline for the next chat architecture upgrade
provides:
  - unified durable chat send command service
  - transactional chat message outbox schema and repository
  - persisted sender acknowledgement contract for REST and WebSocket sends
affects: [chat, websocket, kafka, frontend-reconciliation]
tech-stack:
  added: []
  patterns: [transactional outbox, unified command service, persisted ack contract]
key-files:
  created:
    - backend/src/main/java/com/tradingplatform/chat/service/ChatMessageCommandService.java
    - backend/src/main/java/com/tradingplatform/chat/entity/ChatMessageOutbox.java
    - backend/src/main/java/com/tradingplatform/chat/repository/ChatMessageOutboxRepository.java
    - backend/src/main/resources/db/changelog/013-create-chat-message-outbox.xml
    - backend/src/test/java/com/tradingplatform/chat/service/ChatMessageCommandServiceTest.java
  modified:
    - backend/src/main/java/com/tradingplatform/chat/controller/ChatController.java
    - backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java
    - backend/src/main/java/com/tradingplatform/chat/dto/MessageAck.java
    - backend/src/main/java/com/tradingplatform/chat/dto/SendMessageRequest.java
    - backend/src/main/java/com/tradingplatform/chat/dto/WebSocketMessageRequest.java
    - backend/src/main/java/com/tradingplatform/chat/entity/ChatMessage.java
    - backend/src/main/java/com/tradingplatform/chat/entity/MessageStatus.java
    - backend/src/main/java/com/tradingplatform/chat/service/ChatService.java
    - backend/src/main/resources/db/changelog/006-create-chat-tables.xml
    - backend/src/main/resources/db/changelog/db.changelog-master.xml
    - backend/src/test/java/com/tradingplatform/chat/controller/ChatControllerTest.java
    - backend/src/test/java/com/tradingplatform/chat/controller/ChatWebSocketControllerTest.java
    - backend/src/test/java/com/tradingplatform/chat/repository/MessageRepositoryTest.java
    - backend/src/test/java/com/tradingplatform/chat/service/ChatServiceTest.java
key-decisions:
  - "Message sends now acknowledge durable persistence with `PERSISTED` instead of claiming recipient delivery on the request thread."
  - "REST and WebSocket send entrypoints both call `ChatMessageCommandService.persistMessage(...)` so validation, persistence, unread counts, and outbox writes stay in one transaction."
patterns-established:
  - "Transactional outbox for chat delivery work: persist chat message and outbox row in the same database transaction."
  - "Sender acknowledgements must expose reconciliation fields (`clientMessageId`, `messageId`, `conversationId`, `status`, `createdAt`) immediately after persistence."
requirements-completed: [CHAT-01, CHAT-02, CHAT-04, CHAT-06, CHAT-07]
duration: 9 min
completed: 2026-03-30
---

# Phase 11 Plan 01: Durable Chat Send Foundation Summary

**Unified durable chat send persistence with transactional outbox writes and `PERSISTED` sender acknowledgements for both REST and WebSocket transports**

## Performance

- **Duration:** 9 min
- **Started:** 2026-03-30T15:11:16Z
- **Completed:** 2026-03-30T15:20:10Z
- **Tasks:** 2
- **Files modified:** 18

## Accomplishments
- Added `ChatMessageCommandService` to own chat send validation, message persistence, unread count updates, conversation preview updates, and transactional outbox writes.
- Added the `chat_message_outbox` entity/repository/Liquibase changelog and changed initial chat message state from `SENT` to `PERSISTED`.
- Routed both REST and WebSocket send paths through the shared persisted-ack contract with reconciliation fields for optimistic frontend matching.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add the transactional outbox schema and shared durable send service** - `58e6d10c` (test), `2ab7621d` (feat)
2. **Task 2: Route REST and WebSocket sends through the shared persisted-ack contract** - `5e200fcf` (feat)

## Files Created/Modified
- `backend/src/main/java/com/tradingplatform/chat/service/ChatMessageCommandService.java` - unified durable send command and persisted ack builder
- `backend/src/main/java/com/tradingplatform/chat/entity/ChatMessageOutbox.java` - outbox entity for deferred delivery publication
- `backend/src/main/java/com/tradingplatform/chat/repository/ChatMessageOutboxRepository.java` - pending outbox lookup support
- `backend/src/main/resources/db/changelog/013-create-chat-message-outbox.xml` - schema for transactional outbox rows
- `backend/src/main/java/com/tradingplatform/chat/controller/ChatController.java` - REST send path now returns the persisted ack contract
- `backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java` - WebSocket send path now emits `PERSISTED` ack after durable write
- `backend/src/test/java/com/tradingplatform/chat/service/ChatMessageCommandServiceTest.java` - verifies message + outbox persistence and ack semantics
- `backend/src/test/java/com/tradingplatform/chat/controller/ChatWebSocketControllerTest.java` - verifies websocket ack semantics and absence of direct recipient delivery claim

## Decisions Made
- Used a single command service return envelope so controllers can share one persisted-ack contract while future delivery steps read from the outbox.
- Left Kafka publication and delivery consumers out of this plan; the hot path stops after durable DB writes and the ack.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- A non-clean Maven rerun briefly surfaced a stale `NoClassDefFoundError` during test execution. A targeted `mvn clean test` cleared the incremental build residue and the exact plan verification command then passed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The backend now has a durable outbox foundation ready for ordered publication and async delivery in Plan `11-02`.
- Sender-facing acks are stable and truthful for frontend reconciliation work in later Phase 11 plans.

---
*Phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization*
*Completed: 2026-03-30*

## Self-Check: PASSED

- FOUND: `.planning/phases/11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization/11-01-SUMMARY.md`
- FOUND: `58e6d10c`
- FOUND: `2ab7621d`
- FOUND: `5e200fcf`
