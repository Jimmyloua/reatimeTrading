---
phase: 06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization
verified: 2026-03-25T03:18:00Z
status: human_needed
score: 5/5 must-haves verified
human_verification:
  - test: "Reconnect presence separation"
    expected: "On /messages, transport reconnect state appears separately from seller presence, seller state stays last-known for about 30 seconds, and matching seller rows/header stay visually synchronized."
    why_human: "The code and tests prove stale-window logic and shared state, but not the actual browser UX during a live reconnect across repeated seller rows."
  - test: "Mobile shell and sticky composer"
    expected: "At mobile width, opening a thread replaces the list in-place, a visible `Back to conversations` control returns to the list, the composer stays sticky, and long content does not cause horizontal scrolling."
    why_human: "Automated tests cover route mode and wrapping classes, but safe-area behavior, sticky feel, and overflow in a real browser remain manual checks."
---

# Phase 6: Chat presence reliability, multi-conversation seller status sync, responsive message layout, and Redis-backed realtime optimization Verification Report

**Phase Goal:** Users can rely on seller presence, conversation previews, and message delivery across reconnects, repeated seller threads, multiple backend nodes, and mobile or desktop message layouts.
**Verified:** 2026-03-25T03:18:00Z
**Status:** human_needed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Seller presence survives reconnects and only transitions away from the last-known state after the stale window expires. | ✓ VERIFIED | [`backend/src/main/java/com/tradingplatform/chat/service/PresenceService.java`] stores Redis TTL-backed sessions with a 60s timeout; [`frontend/src/stores/sellerPresenceStore.ts`] applies a 30000ms stale window and `Status updating`; backend `PresenceServiceRedisTest` and frontend `chat-presence-sync.test.tsx` pass. |
| 2 | The same seller presence state stays synchronized across every conversation row and the active thread header. | ✓ VERIFIED | [`frontend/src/stores/sellerPresenceStore.ts`] keys presence by `otherUserId`; [`frontend/src/components/chat/ConversationItem.tsx`] and [`frontend/src/components/chat/ChatView.tsx`] both consume [`frontend/src/hooks/useConversationPresence.ts`]; `chat-presence-sync.test.tsx` proves one shared subscription drives repeated rows and header together. |
| 3 | Realtime previews and unread counts remain duplicate-safe during reconnects and REST fallback sends. | ✓ VERIFIED | [`frontend/src/stores/chatStore.ts`] dedupes by `message.id` and only reorders on newer `lastMessageAt`; [`frontend/src/hooks/useChat.ts`] rehydrates on reconnect and refreshes conversation list metadata after fallback sends; `chat-realtime-fallback.test.tsx` and `messages-page-routing.test.tsx` pass. |
| 4 | The `/messages` route switches between desktop/two-pane and mobile/single-pane modes without losing thread context. | ✓ VERIFIED | [`frontend/src/pages/MessagesPage.tsx`] uses viewport-based shell switching while preserving `?conversation=` routing and mobile `Back to conversations`; [`frontend/src/components/chat/MessageBubble.tsx`] and [`frontend/src/components/chat/MessageInput.tsx`] implement responsive bubble and composer behavior; `messages-responsive-layout.test.tsx` and `messages-page-routing.test.tsx` pass. |
| 5 | Redis-backed fan-out distributes message and presence events across nodes while MySQL remains the durable source of truth. | ✓ VERIFIED | [`backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java`] persists via `chatService.sendMessage(...)` before `publishMessageDelivery(...)`; [`backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventPublisher.java`], [`backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventSubscriber.java`], and [`backend/src/main/java/com/tradingplatform/config/RedisPubSubConfig.java`] wire Redis channels to websocket destinations; backend tests pass under JDK 21. |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `backend/src/main/java/com/tradingplatform/chat/service/PresenceService.java` | Redis-backed presence session tracking | ✓ VERIFIED | Exists, substantive, and referenced by controller/maintenance plus verified by Redis tests. |
| `backend/src/main/java/com/tradingplatform/chat/service/PresenceSessionMaintenance.java` | Timeout-driven offline broadcast path | ✓ VERIFIED | Exists, publishes presence timeout transitions through Redis publisher. |
| `backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java` | Persistence-first send path and presence connect/disconnect publishing | ✓ VERIFIED | Exists, writes first through `chatService`, publishes message/presence fan-out second. |
| `backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventPublisher.java` | Redis publish path for message/presence fan-out | ✓ VERIFIED | Exists, publishes `MESSAGE_DELIVERY` and `PRESENCE_UPDATE` to dedicated channels. |
| `backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventSubscriber.java` | Redis-to-websocket forwarding | ✓ VERIFIED | Exists, forwards to `/queue/messages` and `/topic/presence.{userId}`. |
| `backend/src/main/java/com/tradingplatform/config/RedisPubSubConfig.java` | Redis listener container subscribed to both channels | ✓ VERIFIED | Exists and subscribes to message and presence channels. |
| `frontend/src/stores/sellerPresenceStore.ts` | Shared seller-level presence store | ✓ VERIFIED | Exists, substantive, and used by `useConversationPresence`. |
| `frontend/src/hooks/useConversationPresence.ts` | Shared seller presence consumption and stale transport mapping | ✓ VERIFIED | Exists, substantive, and consumed by row and header components. |
| `frontend/src/stores/chatStore.ts` | Duplicate-safe message and preview state | ✓ VERIFIED | Exists, dedupes messages and gates reorder on timestamp. |
| `frontend/src/hooks/useChat.ts` | Reconnect rehydrate and degraded-only fallback refresh | ✓ VERIFIED | Exists and handles websocket subscriptions, degraded polling, reconnect rehydrate, and REST fallback reconciliation. |
| `frontend/src/pages/MessagesPage.tsx` | Responsive shell with mobile back navigation and query-param routing | ✓ VERIFIED | Exists and is exercised by responsive and routing tests. |
| `frontend/src/components/chat/MessageBubble.tsx` | Responsive bubble widths and long-content wrapping | ✓ VERIFIED | Exists with `max-w-[85%] md:max-w-[70%]` and wrap controls. |
| `frontend/src/components/chat/MessageInput.tsx` | Sticky composer with safe-area padding and degraded helper copy | ✓ VERIFIED | Exists with sticky positioning and safe-area bottom padding. |
| `backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java` | Proof of Redis TTL/session behavior | ✓ VERIFIED | Exists and passes under JDK 21. |
| `backend/src/test/java/com/tradingplatform/chat/integration/RedisChatFanoutIntegrationTest.java` | Proof of Redis fan-out contract | ✓ VERIFIED | Exists and passes under JDK 21. |
| `frontend/src/tests/chat-presence-sync.test.tsx` | Proof of shared seller presence sync | ✓ VERIFIED | Exists and passes. |
| `frontend/src/tests/chat-realtime-fallback.test.tsx` | Proof of duplicate-safe realtime fallback behavior | ✓ VERIFIED | Exists and passes. |
| `frontend/src/tests/messages-responsive-layout.test.tsx` | Proof of responsive messages shell behavior | ✓ VERIFIED | Exists and passes. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `ChatWebSocketController.java` | `RedisChatEventPublisher.java` | `publishMessageDelivery(...)` after `chatService.sendMessage(...)` | ✓ WIRED | Persistence-first ordering is explicit in `sendMessage`. |
| `PresenceSessionMaintenance.java` | `RedisChatEventPublisher.java` | `publishPresenceUpdate(...)` for timed-out sessions | ✓ WIRED | Offline timeout transitions are rebroadcast through Redis fan-out. |
| `RedisPubSubConfig.java` | `RedisChatEventSubscriber.java` | Redis listener container on both realtime channels | ✓ WIRED | Subscribed to `chat:realtime:message` and `chat:realtime:presence`. |
| `RedisChatEventSubscriber.java` | websocket users/topics | `convertAndSendToUser(..., "/queue/messages", ...)` and `convertAndSend("/topic/presence.{userId}", ...)` | ✓ WIRED | Message and presence events both fan out locally. |
| `sellerPresenceStore.ts` | `useConversationPresence.ts` | seller-keyed shared snapshot + subscription | ✓ WIRED | Hook seeds, subscribes, and applies transport stale-window logic from shared store. |
| `useConversationPresence.ts` | `ConversationItem.tsx` | shared row presence rendering | ✓ WIRED | Row presence copy comes from the shared seller store. |
| `useConversationPresence.ts` | `ChatView.tsx` | shared active-header presence rendering | ✓ WIRED | Header presence copy comes from the same seller store. |
| `useChat.ts` | `chatStore.ts` | dedupe, preview sync, unread reconciliation | ✓ WIRED | Incoming websocket and fallback flows update centralized conversation/message state. |
| `MessagesPage.tsx` | `ConversationList.tsx` | desktop/tablet sidebar and mobile list mode | ✓ WIRED | Pane visibility and selection state flow through route query params. |
| `MessagesPage.tsx` | `ChatView.tsx` | selected conversation and mobile thread mode | ✓ WIRED | Active thread persists through `?conversation=` and mobile back removes it. |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| P6-01 | `06-00`, `06-01` | Seller presence survives reconnects and only transitions offline after timeout/stale exhaustion | ✓ SATISFIED | Redis TTL/session logic in [`backend/src/main/java/com/tradingplatform/chat/service/PresenceService.java`], timeout rebroadcast in [`backend/src/main/java/com/tradingplatform/chat/service/PresenceSessionMaintenance.java`], stale-window rendering in [`frontend/src/stores/sellerPresenceStore.ts`], passing `PresenceServiceRedisTest` and `chat-presence-sync.test.tsx`. |
| P6-02 | `06-00`, `06-02` | Presence state stays synchronized across all rows and active header | ✓ SATISFIED | Shared seller-keyed store in [`frontend/src/stores/sellerPresenceStore.ts`], shared hook in [`frontend/src/hooks/useConversationPresence.ts`], row/header consumers in [`frontend/src/components/chat/ConversationItem.tsx`] and [`frontend/src/components/chat/ChatView.tsx`], passing `chat-presence-sync.test.tsx`. |
| P6-03 | `06-00`, `06-02` | Previews/unread counts remain duplicate-safe during reconnects and fallback sends | ✓ SATISFIED | Deduping and timestamp-gated reorder in [`frontend/src/stores/chatStore.ts`], reconnect/fallback reconciliation in [`frontend/src/hooks/useChat.ts`], passing `chat-realtime-fallback.test.tsx` and `messages-page-routing.test.tsx`. |
| P6-04 | `06-00`, `06-03` | `/messages` switches between desktop/two-pane and mobile/single-pane layouts without losing thread context | ✓ SATISFIED | Responsive shell in [`frontend/src/pages/MessagesPage.tsx`], bubble/input layout in [`frontend/src/components/chat/MessageBubble.tsx`] and [`frontend/src/components/chat/MessageInput.tsx`], passing `messages-responsive-layout.test.tsx` and `messages-page-routing.test.tsx`. |
| P6-05 | `06-00`, `06-01` | Redis fan-out distributes realtime events while MySQL remains durable source of truth | ✓ SATISFIED | Persistence-first message send in [`backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java`], Redis contracts in [`backend/src/main/java/com/tradingplatform/chat/redis/RedisChannels.java`], [`backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventPublisher.java`], [`backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventSubscriber.java`], and passing backend tests. |

### Anti-Patterns Found

No blocker or warning anti-patterns found in the phase implementation files scanned. The grep pass did not surface TODO/FIXME placeholders, empty stub returns, or placeholder copy in the verified artifacts.

### Human Verification Required

### 1. Reconnect Presence Separation

**Test:** Open `/messages`, use a seller that appears in multiple conversation rows, then force websocket reconnect/disconnect transitions.
**Expected:** Seller presence stays last-known first, transport state changes separately, `Status updating` appears only after the stale window, and repeated seller rows match the active header.
**Why human:** The store logic and tests prove state transitions, but not the final visual behavior and timing in a live browser.

### 2. Mobile Shell And Sticky Composer

**Test:** Resize to mobile width, open a conversation, navigate back with `Back to conversations`, then scroll through long messages and long URLs.
**Expected:** The thread replaces the list on the same route, the back control returns to the list, the composer remains sticky with safe-area padding, and there is no horizontal scrolling.
**Why human:** Automated coverage verifies route mode and wrapping classes, but actual mobile layout feel and overflow behavior still require a browser check.

### Gaps Summary

No automated implementation gaps were found. Phase 6 appears complete in code and targeted tests, but the remaining acceptance risk is manual UX verification for reconnect-state presentation and mobile browser layout behavior.

---

_Verified: 2026-03-25T03:18:00Z_
_Verifier: Claude (gsd-verifier)_
