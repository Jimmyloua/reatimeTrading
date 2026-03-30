---
phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
plan: 03
subsystem: ui
tags: [chat, react, zustand, websocket, reconciliation, cursor]
requires:
  - phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
    plan: 01
    provides: persisted ack contract and durable chat send flow
  - phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
    plan: 02
    provides: afterMessageId reconnect cursor and async delivery pipeline
provides:
  - optimistic sender ack reconciliation using clientMessageId
  - reconnect catch-up that appends only missing persisted messages
  - frontend contract updates for persisted acknowledgements and cursor fetches
affects: [chat, websocket, reconnect, optimistic-ui]
tech-stack:
  added: []
  patterns: [client-side ack reconciliation, cursor-based reconnect catch-up, optimistic message identity replacement]
key-files:
  created:
    - frontend/src/tests/chat-message-ack-reconciliation.test.tsx
    - frontend/src/tests/chat-reconnect-catchup.test.tsx
  modified:
    - frontend/src/hooks/useChat.ts
    - frontend/src/stores/chatStore.ts
    - frontend/src/types/chat.ts
    - frontend/src/api/chatApi.ts
    - frontend/src/tests/chat-realtime-fallback.test.tsx
key-decisions:
  - "Ack events now reconcile optimistic rows locally instead of triggering a full thread refresh."
  - "Reconnect uses the latest persisted message id as the exact afterMessageId cursor to fetch only missing messages."
  - "REST fallback reuses the same MessageAck contract as websocket delivery so connected and disconnected send paths converge."
patterns-established:
  - "Optimistic chat messages carry clientMessageId until the persisted ack replaces the temporary identity."
  - "Reconnect state updates append unseen persisted messages and preserve existing thread state instead of resetting it."
requirements-completed: [CHAT-02, CHAT-03, P6-03, P6-04]
duration: 24 min
completed: 2026-03-30
---

# Phase 11 Plan 03: Frontend Reconciliation Summary

**Frontend chat now reconciles optimistic sends from persisted ack payloads and catches up reconnect gaps with `afterMessageId` instead of broad refreshes.**

## Performance

- **Duration:** 24 min
- **Started:** 2026-03-30T15:25:00Z
- **Completed:** 2026-03-30T15:49:00Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Added a shared `MessageAck` contract and `clientMessageId` correlation so optimistic chat sends can be replaced in place when persistence completes.
- Updated `useChat` and the chat store to remove ack-driven full refreshes and instead reconcile local optimistic rows while keeping conversation previews in sync.
- Added reconnect catch-up support via `afterMessageId`, plus targeted tests proving delta append behavior and duplicate-safe reconciliation.

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace ack-driven full refresh with optimistic message reconciliation** - `f2f70ab5` (feat)
2. **Task 2: Add reconnect catch-up using persisted message cursors** - `f2f70ab5` (feat)

## Files Created/Modified
- `frontend/src/types/chat.ts` - adds `MessageAck`, `clientMessageId`, and `PERSISTED` status support
- `frontend/src/api/chatApi.ts` - returns persisted ack payloads from sends and forwards `afterMessageId` on message fetches
- `frontend/src/stores/chatStore.ts` - reconciles optimistic rows in place and exposes latest persisted message cursor helpers
- `frontend/src/hooks/useChat.ts` - swaps refresh-based ack handling for reconciliation and delta catch-up flows
- `frontend/src/tests/chat-message-ack-reconciliation.test.tsx` - proves optimistic rows are replaced by persisted acknowledgements
- `frontend/src/tests/chat-reconnect-catchup.test.tsx` - proves reconnect fetches only messages newer than the persisted cursor
- `frontend/src/tests/chat-realtime-fallback.test.tsx` - updates fallback expectations to the persisted ack contract

## Decisions Made
- Kept the optimistic send experience intact for both websocket and REST fallback paths by unifying both around the same `MessageAck` payload.
- Reused store-level dedupe semantics when appending reconnect deltas so Phase 6 duplicate protections remain active.
- Limited reconnect refresh scope to the active thread message delta and preview sync rather than reloading conversation collections.

## Deviations from Plan

None on scope. The existing fallback test was tightened to assert the new persisted-ack behavior instead of the removed full-refresh path.

## Issues Encountered

- An older fallback assertion still expected a second `getMessages` reload after ack delivery.
- The fix was to align that test with the new reconciliation contract and verify no extra refresh occurs.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Wave 4 can now verify the full durable send lifecycle across backend persistence, async delivery, frontend reconciliation, and reconnect catch-up.
- The active conversation flow no longer depends on broad refreshes after each send acknowledgement.

## Self-Check: PASSED

- FOUND: `.planning/phases/11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization/11-03-SUMMARY.md`
- FOUND: `f2f70ab5`
- FOUND: `frontend/src/tests/chat-message-ack-reconciliation.test.tsx`
- FOUND: `frontend/src/tests/chat-reconnect-catchup.test.tsx`
