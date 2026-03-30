---
phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
plan: 02
subsystem: backend
tags: [chat, kafka, outbox, websocket, cursor]
requires:
  - phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
    plan: 01
    provides: durable chat send command, persisted acknowledgements, and transactional outbox rows
provides:
  - outbox-to-kafka relay keyed by conversationId
  - async Kafka consumer that advances delivery state and pushes recipients
  - afterMessageId cursor support for reconnect catch-up
affects: [chat, kafka, websocket, reconnect]
tech-stack:
  added: []
  patterns: [transactional outbox relay, conversation-keyed delivery stream, cursor-based catch-up]
key-files:
  created:
    - backend/src/main/java/com/tradingplatform/chat/kafka/ChatDeliveryEvent.java
    - backend/src/main/java/com/tradingplatform/chat/kafka/ChatDeliveryConsumer.java
    - backend/src/main/java/com/tradingplatform/chat/outbox/ChatOutboxRelay.java
    - backend/src/main/java/com/tradingplatform/chat/repository/ChatMessageRepository.java
    - backend/src/main/java/com/tradingplatform/chat/service/ChatDeliveryStatusService.java
    - backend/src/main/java/com/tradingplatform/chat/service/ChatQueryService.java
    - backend/src/test/java/com/tradingplatform/chat/integration/ChatOutboxRelayTest.java
    - backend/src/test/java/com/tradingplatform/chat/integration/ChatDeliveryConsumerTest.java
  modified:
    - backend/src/main/java/com/tradingplatform/chat/controller/ChatController.java
    - backend/src/test/java/com/tradingplatform/chat/controller/ChatControllerTest.java
key-decisions:
  - "Kafka delivery events are published with conversationId as the record key so in-thread ordering stays stable."
  - "Recipient websocket fan-out and delivery-status advancement now belong to the async Kafka consumer instead of the original request thread."
  - "Reconnect catch-up uses the exact afterMessageId cursor on the conversation messages API instead of broad history refreshes."
patterns-established:
  - "Outbox relay marks rows PUBLISHED only after Kafka publish succeeds and records FAILED status with incremented attempts when publication throws."
  - "Duplicate Kafka deliveries are idempotent at the consumer boundary by checking whether the message already reached DELIVERED."
requirements-completed: [CHAT-02, CHAT-03, CHAT-04, CHAT-05]
duration: 18 min
completed: 2026-03-30
---

# Phase 11 Plan 02: Async Delivery Pipeline Summary

**Kafka now owns durable chat delivery after persistence, and reconnect catch-up can request only messages newer than `afterMessageId`.**

## Performance

- **Duration:** 18 min
- **Started:** 2026-03-30T15:12:00Z
- **Completed:** 2026-03-30T15:34:00Z
- **Tasks:** 3
- **Files modified:** 10

## Accomplishments
- Added `ChatOutboxRelay` and `ChatDeliveryEvent` so pending outbox rows publish to `chat.message.persisted` with the exact Kafka key `conversationId`.
- Added `ChatDeliveryConsumer` and `ChatDeliveryStatusService` so async delivery advances persisted messages to `DELIVERED`, pushes `/user/queue/messages`, and emits notifications outside the request thread.
- Added `ChatQueryService`, `ChatMessageRepository`, and controller support for `afterMessageId` so reconnects can fetch only missing messages in ascending order.

## Task Commits

Each task was committed atomically:

1. **Task 1: Relay pending outbox rows to Kafka with conversation-key ordering** - `e9327f78` (feat)
2. **Task 2: Consume Kafka delivery events, push online recipients, and update delivery state** - `27c69c0c` (feat)
3. **Task 3: Add backend delta-retrieval support for reconnect catch-up by `afterMessageId`** - `52fd293d` (feat)
4. **Verification support fix:** `93c02d8d` (test)

## Files Created/Modified
- `backend/src/main/java/com/tradingplatform/chat/outbox/ChatOutboxRelay.java` - drains pending outbox rows and publishes Kafka delivery events keyed by conversation
- `backend/src/main/java/com/tradingplatform/chat/kafka/ChatDeliveryEvent.java` - delivery payload contract containing messageId, conversationId, recipientUserId, and status
- `backend/src/main/java/com/tradingplatform/chat/kafka/ChatDeliveryConsumer.java` - async consumer that pushes live recipients and advances message lifecycle to `DELIVERED`
- `backend/src/main/java/com/tradingplatform/chat/service/ChatDeliveryStatusService.java` - idempotent delivery-state advancement for persisted chat messages
- `backend/src/main/java/com/tradingplatform/chat/service/ChatQueryService.java` - participant-safe delta retrieval and recipient delivery lookup
- `backend/src/main/java/com/tradingplatform/chat/controller/ChatController.java` - accepts `afterMessageId` on message retrieval without breaking the full-history path
- `backend/src/test/java/com/tradingplatform/chat/integration/ChatOutboxRelayTest.java` - proves Kafka publication uses conversationId as the record key
- `backend/src/test/java/com/tradingplatform/chat/integration/ChatDeliveryConsumerTest.java` - proves async recipient push and duplicate-delivery idempotency

## Decisions Made
- Kept Redis presence and typing untouched while moving durable message delivery onto Kafka.
- Reused the persisted outbox payload shape from Plan 11-01 so the relay could deserialize directly into a delivery event without inventing another intermediate contract.
- Isolated controller verification from background Redis maintenance and Kafka listener startup at the test boundary rather than adding app-level fallbacks.

## Deviations from Plan

None on scope. A small test-only isolation fix was added after the first verification run exposed unrelated broker background noise in `ChatControllerTest`.

## Issues Encountered

- `ChatControllerTest` initially booted background Redis maintenance and the Kafka listener, which produced noisy connection errors even though the targeted assertions passed.
- The fix was to mock `PresenceSessionMaintenance` and `ChatDeliveryConsumer` in the controller test so the verification command stays focused on the HTTP contract.

## Verification

- `cd backend && mvn "-Dtest=ChatOutboxRelayTest,ChatDeliveryConsumerTest,ChatControllerTest" test`

## Next Phase Readiness

- The backend now exposes the persisted ack plus `afterMessageId` contracts that the frontend reconciliation work in Plan `11-03` needs.
- Async delivery is no longer coupled to request-thread websocket fan-out, so Wave 3 can safely optimize client-side reconciliation and reconnect behavior.

## Self-Check: PASSED

- FOUND: `.planning/phases/11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization/11-02-SUMMARY.md`
- FOUND: `e9327f78`
- FOUND: `27c69c0c`
- FOUND: `52fd293d`
- FOUND: `93c02d8d`
