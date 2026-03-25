---
phase: 06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization
plan: 02
subsystem: ui
tags: [react, zustand, websocket, chat, vitest]
requires:
  - phase: 06-00
    provides: frontend red-test scaffolding for chat presence and fallback behavior
  - phase: 06-01
    provides: Redis-backed backend presence and cross-node realtime fan-out contract
provides:
  - shared seller presence cache keyed by otherUserId
  - stale-aware presence hook and synchronized row/header presence rendering
  - duplicate-safe chat message application and reconnect rehydrate behavior
  - degraded-only fallback refresh with conversation list reconciliation
affects: [messages-page, chat-ui, realtime-reliability, seller-presence]
tech-stack:
  added: []
  patterns: [seller-level presence store, duplicate-safe message application, degraded-only refresh polling]
key-files:
  created: [frontend/src/stores/sellerPresenceStore.ts]
  modified: [frontend/src/types/chat.ts, frontend/src/hooks/useConversationPresence.ts, frontend/src/components/chat/ConversationItem.tsx, frontend/src/components/chat/ConversationList.tsx, frontend/src/components/chat/ChatView.tsx, frontend/src/stores/chatStore.ts, frontend/src/hooks/useChat.ts, frontend/src/tests/chat-presence-sync.test.tsx, frontend/src/tests/chat-realtime-fallback.test.tsx, frontend/src/tests/messages-page-routing.test.tsx]
key-decisions:
  - "Seller presence is owned by a shared seller-level store keyed by otherUserId instead of per-mounted components."
  - "Reconnect rehydrate refreshes the active thread and the authoritative conversation list once, while polling remains degraded-mode only."
patterns-established:
  - "Seller presence and transport state render independently so reconnects do not force a seller offline."
  - "Realtime message handling dedupes by message.id before unread-count or preview side effects are applied."
requirements-completed: [P6-02, P6-03]
duration: 16min
completed: 2026-03-25
---

# Phase 06 Plan 02: Shared seller presence sync and duplicate-safe reconnect fallback Summary

**Shared seller presence caching with stale-aware transport handling, duplicate-safe chat updates, and reconnect/fallback reconciliation for conversation previews and unread metadata**

## Performance

- **Duration:** 16 min
- **Started:** 2026-03-25T01:39:30Z
- **Completed:** 2026-03-25T01:55:04Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- Added a seller-level presence store with a 30000 ms stale window so repeated seller rows and the active header render the same presence copy.
- Split transport status from seller availability in the chat UI and removed the unconditional 10-second active-thread polling loop from connected mode.
- Made realtime message handling idempotent by `message.id` and rehydrated the active thread plus conversation-list metadata on reconnect or degraded REST sends.

## Task Commits

1. **Task 1: Create a seller-level shared presence store and stale-window hook per P6-02** - `97f5abb6` (`test`), `67c7931d` (`feat`)
2. **Task 2: Make realtime updates duplicate-safe and restrict refresh polling to degraded mode per P6-03** - `fe65eb2a` (`test`), `0b68dc13` (`feat`)

## Files Created/Modified
- `frontend/src/stores/sellerPresenceStore.ts` - Shared seller presence cache, stale timer handling, and one-toast-per-seller-session guard.
- `frontend/src/hooks/useConversationPresence.ts` - Seller-keyed presence subscription, stale fallback mapping, and shared snapshot consumption.
- `frontend/src/components/chat/ConversationItem.tsx` - Shared presence pill rendering in list rows.
- `frontend/src/components/chat/ChatView.tsx` - Shared seller presence pill and degraded transport helper rendering without always-on polling.
- `frontend/src/stores/chatStore.ts` - Message dedupe, timestamp-gated preview reordering, and seen-message tracking.
- `frontend/src/hooks/useChat.ts` - Reconnect rehydrate, degraded-only refresh interval, and fallback send reconciliation.
- `frontend/src/tests/chat-presence-sync.test.tsx` - Shared seller presence and stale-window coverage.
- `frontend/src/tests/chat-realtime-fallback.test.tsx` - Duplicate-safe reconnect and degraded-send reconciliation coverage.
- `frontend/src/tests/messages-page-routing.test.tsx` - Routed conversation accuracy after list rehydrate coverage.

## Decisions Made
- Seller presence now persists at the seller level, keyed by `otherUserId`, so repeated conversations with the same seller cannot drift apart.
- Reconnect and degraded-send recovery use authoritative API rehydrate for the active thread plus conversation list, rather than resuming connected-mode polling.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed render-loop reseeding in the shared presence hook**
- **Found during:** Task 1
- **Issue:** The initial shared-store hook reseeded presence from a fresh object dependency each render, which caused unnecessary notifications and a hanging test run.
- **Fix:** Stabilized hook inputs to primitive dependencies, short-circuited unchanged snapshots, and cleared stale timers when the last subscriber unmounted.
- **Files modified:** `frontend/src/hooks/useConversationPresence.ts`, `frontend/src/stores/sellerPresenceStore.ts`, `frontend/src/tests/chat-presence-sync.test.tsx`
- **Verification:** `cd frontend && npm test -- --run src/tests/chat-presence-sync.test.tsx`
- **Committed in:** `67c7931d`

**2. [Rule 1 - Bug] Tracked duplicate message IDs for non-active conversations before unread side effects**
- **Found during:** Task 2
- **Issue:** The first dedupe pass only protected active-thread bubbles; duplicate websocket deliveries for background conversations could still bump unread counts and replay preview updates.
- **Fix:** Added shared seen-message tracking in the chat store and used it in `useChat` before preview/unread side effects for non-active conversation events.
- **Files modified:** `frontend/src/stores/chatStore.ts`, `frontend/src/hooks/useChat.ts`
- **Verification:** `cd frontend && npm test -- --run src/tests/chat-realtime-fallback.test.tsx src/tests/messages-page-routing.test.tsx`
- **Committed in:** `0b68dc13`

---

**Total deviations:** 2 auto-fixed (2 bug fixes)
**Impact on plan:** Both fixes were required for correctness. Scope stayed within the planned frontend presence and realtime reliability work.

## Issues Encountered
- The first shared-presence implementation introduced a render-loop style reseed path; correcting effect dependencies and store cleanup resolved it.
- Background-conversation duplicate events needed a separate seen-message guard because only active-thread messages were initially entering the message list.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Presence sync, reconnect rehydrate, and degraded fallback behavior are covered by frontend tests and ready for responsive `/messages` layout work in Plan 06-03.
- No blockers identified for the next plan.

## Self-Check: PASSED
- Found `.planning/phases/06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization/06-02-SUMMARY.md`
- Found commits `97f5abb6`, `67c7931d`, `fe65eb2a`, and `0b68dc13`
