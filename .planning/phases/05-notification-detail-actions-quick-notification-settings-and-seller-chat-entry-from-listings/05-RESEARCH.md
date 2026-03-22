# Phase 5: Notification detail actions, quick notification settings, and seller chat entry from listings - Research

**Researched:** 2026-03-22
**Domain:** Notification navigation, notification preferences, and chat bootstrap on the existing React + Spring real-time stack
**Confidence:** MEDIUM

## User Constraints

No `CONTEXT.md` exists for this phase.

### Locked Decisions
None.

### Claude's Discretion
None explicitly recorded. Scope must align with the roadmap phase title and existing CHAT/NOTF architecture.

### Deferred Ideas (OUT OF SCOPE)
None explicitly recorded.

## Summary

Phase 5 should be planned as an integration phase on top of existing Phase 3 and Phase 4 primitives, not as a new subsystem. The current codebase already has the minimum building blocks for notification detail actions and seller-chat entry: notifications already carry `referenceId` and `referenceType`, conversations are already idempotent per `listingId + buyerId`, and the listing detail page already exposes seller identity. What is missing is the navigation layer, preference storage, and a stable way to deep-link into a specific conversation from both notifications and listing surfaces.

The biggest planning mistake would be to treat notifications and chat as isolated UI work. They are coupled by unread state, routing, and message bootstrap behavior. Today the notification dropdown mostly shows only in-session pushed notifications, the messages page cannot deep-link to a specific conversation, and inactive conversations do not get updated in client state when new real-time messages arrive. If Phase 5 adds detail actions without fixing those seams, users will click a notification and still have to manually locate the right conversation or see stale unread counts.

**Primary recommendation:** Plan this phase in three slices: `1)` deep-linkable notification actions, `2)` persisted in-app notification preferences, `3)` seller chat entry from listing detail that reuses idempotent conversation creation and lands directly in the target conversation.

## Concrete Gaps

### Frontend Gaps

| Area | Current State | Gap |
|------|---------------|-----|
| Notification item actions | `NotificationItem` accepts `onClick`, but list/dropdown do not wire navigation | Notification details cannot open the referenced conversation/listing/transaction |
| Notification dropdown data | `NotificationDropdown` reads store state only; `useNotifications()` loads unread count, not recent history | Dropdown can be empty until a WebSocket event arrives in this session |
| Messages deep link | [`MessagesPage.tsx`](../../../../frontend/src/pages/MessagesPage.tsx) uses local `activeConversationId` only | No route/query-param bootstrap for notification or listing-driven entry |
| Chat unread sync | [`useChat.ts`](../../../../frontend/src/hooks/useChat.ts) only appends messages for the active conversation | Inactive conversations do not update preview/unread state in real time |
| Conversation read sync | Viewing a conversation marks backend messages read, but local conversation unread badge is not explicitly cleared on select | UI can show stale unread counts until a refetch |
| Listing seller chat entry | [`ListingDetailPage.tsx`](../../../../frontend/src/pages/ListingDetailPage.tsx) links seller profile only | No direct "message seller" entry from seller identity surface |
| Listing list seller entry | [`ListingCard.tsx`](../../../../frontend/src/components/ListingCard.tsx) has no seller summary data | Seller chat entry from browse cards would require upstream API expansion |

### Backend Gaps

| Area | Current State | Gap |
|------|---------------|-----|
| Notification preferences | No entity, repository, service, or endpoint exists | Quick settings cannot persist or affect backend behavior |
| Notification detail model | Notifications only expose `referenceId` and free-form `referenceType` | Frontend must infer actions; string normalization is inconsistent |
| Reference normalization | Chat pushes `"conversation"` and `"listing"`, transaction services write `"TRANSACTION"` | Action routing will be fragile unless normalized |
| Notification suppression | `NotificationService.createNotification(...)` always saves | No way to suppress categories when user disables them |
| Transaction detail actionability | Transaction notifications already carry transaction IDs | Good enough for deep links once frontend wiring exists |

## Candidate Phase Requirements

These are the likely missing requirements this phase should formalize instead of trying to stretch old CHAT/NOTF requirements:

| ID | Description | Why It Exists |
|----|-------------|---------------|
| NOTF-05 | User can click a notification and open the relevant conversation, listing, or transaction detail | Existing notifications have references but no action flow |
| NOTF-06 | User can manage in-app notification preferences from a quick settings entry point | No preference model exists today |
| NOTF-07 | Disabled notification categories suppress future in-app notifications without affecting core data flows | Keeps chat/transaction state intact while reducing notification noise |
| CHAT-06 | User can start or resume a seller conversation directly from listing detail seller information | Existing CHAT-01 is generic, but listing surfaces do not expose an entry point |
| CHAT-07 | Deep links can open Messages on a specific conversation | Required for notifications and seller entry to feel complete |

## Candidate Success Criteria

1. Clicking a `NEW_MESSAGE` notification opens `/messages` with the correct conversation selected and unread badges clear immediately.
2. Clicking an `ITEM_SOLD` or `TRANSACTION_UPDATE` notification opens the relevant listing or transaction detail when a valid reference exists.
3. The notification dropdown shows recent persisted notifications, not just those received during the current browser session.
4. A user can open quick notification settings, toggle in-app categories, refresh, and see the same preference state.
5. Disabling a notification category stops future notifications of that category from being created or pushed, while chat message persistence and conversation unread counts still work.
6. Clicking seller information on another user's listing detail page creates or reuses the conversation and lands in the correct chat thread.
7. Self-chat remains blocked.

## Proposed Scope Slices

### Slice 1: Notification Detail Actions

- Add notification action resolution on the frontend from `type + referenceType + referenceId`
- Support at minimum:
  - `NEW_MESSAGE` -> conversation
  - `ITEM_SOLD` -> listing or transaction detail depending on stored reference
  - `TRANSACTION_UPDATE` -> transaction detail
- Make `/messages` deep-linkable via query param such as `?conversation=123`
- Load the target conversation if it is not already in store

### Slice 2: Quick Notification Settings

- Add a user-scoped backend preference model for in-app notifications only
- Start narrow:
  - `NEW_MESSAGE`
  - `ITEM_SOLD`
  - `TRANSACTION_UPDATE`
- Add a quick settings affordance from the notification dropdown and/or notifications page
- Preference change must be persisted server-side, not only in Zustand/localStorage

### Slice 3: Seller Chat Entry From Listing Detail

- Add seller-chat CTA on listing detail seller card for authenticated non-owners
- Reuse existing `POST /api/conversations` idempotency
- After conversation creation/retrieval, navigate directly to `/messages?conversation={id}`

### Explicitly Avoid In This Phase

- Seller chat entry from browse listing cards unless the listing summary API is expanded intentionally
- Email/push/mobile notification channels
- Notification center redesign beyond the flows needed for actions and quick settings
- New chat transport or new state library

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `react-router-dom` | 7.13.1 | Route/query-param driven chat bootstrap and notification actions | Already in repo; official APIs support URL-backed state for deep links |
| `@tanstack/react-query` | 5.94.5 | Server-state fetch/invalidation for preferences and targeted conversation loads | Already project standard for server state; better than hand-rolled fetch sync |
| `zustand` | 5.0.12 | Ephemeral UI/chat store coordination | Already used for chat/notification state |
| Spring Boot Web + Security + WebSocket | repo uses 3.4.2 | Existing REST + STOMP stack | Already powers chat and notifications |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `date-fns` | 4.1.0 | Relative notification timestamps | Already used in `NotificationItem` |
| STOMP user destinations | Spring Framework docs | User-targeted notification and chat delivery | Keep existing `/user/queue/...` routing |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Query-param driven `/messages?conversation=...` | Nested route `/messages/:conversationId` | Cleaner URL, but larger routing/page refactor than this phase needs |
| Dedicated notification action fields in DB | Frontend-derived actions from current references | Current schema is enough for this phase if reference types are normalized |
| JSON user preference column | Dedicated `notification_preferences` table | JSON is faster to add, table is clearer and more testable for fixed categories |

**Installation:**
```bash
# No new frontend library is required for the recommended scope.
# Backend likely needs only a Liquibase migration plus existing Spring/JPA stack.
```

**Version verification:** Verified against npm registry on 2026-03-22.
- `react-router-dom` latest: `7.13.1` published 2026-02-23
- `@tanstack/react-query` latest: `5.94.5` published 2026-03-21
- `zustand` latest: `5.0.12` published 2026-03-16
- `date-fns` latest: `4.1.0` published 2024-09-17

## Architecture Patterns

### Recommended Project Structure

```text
frontend/src/
├── pages/
│   ├── NotificationsPage.tsx      # full notification center + settings entry
│   └── MessagesPage.tsx           # query-param bootstrap for target conversation
├── components/
│   ├── notifications/             # item action wiring, dropdown quick settings
│   └── chat/                      # conversation bootstrap + list unread sync
├── api/
│   ├── notificationApi.ts         # detail action helpers + preferences endpoints
│   └── chatApi.ts                 # targeted conversation fetch/create flow
└── stores/
    ├── notificationStore.ts       # ephemeral recent notifications
    └── chatStore.ts               # active conversation + unread coordination

backend/src/main/java/com/tradingplatform/
├── notification/
│   ├── controller/                # history + preferences endpoints
│   ├── service/                   # create/push/suppress notifications
│   ├── entity/                    # notification + preferences
│   └── repository/                # preference reads/writes
└── chat/
    └── service/                   # conversation reuse remains here
```

### Pattern 1: URL-Backed Conversation Bootstrap

**What:** Use the URL as the source of truth for the target conversation when navigation comes from notifications or listings.

**When to use:** Any flow that enters chat from outside the messages page.

**Example:**
```tsx
import { useSearchParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { chatApi } from '@/api/chatApi'

export default function MessagesPage() {
  const [params, setParams] = useSearchParams()
  const [activeConversationId, setActiveConversationId] = useState<number | null>(null)

  useEffect(() => {
    const raw = params.get('conversation')
    const id = raw ? Number(raw) : null
    if (!id || Number.isNaN(id)) return
    setActiveConversationId(id)
  }, [params])

  const selectConversation = (id: number) => {
    setActiveConversationId(id)
    setParams({ conversation: String(id) })
  }
}
```
Source: React Router `useSearchParams` docs - https://reactrouter.com/api/hooks/useSearchParams

### Pattern 2: Backend-Enforced Notification Preference Check

**What:** Enforce preference suppression before creating and pushing notifications.

**When to use:** Any future notification-producing path, especially message and transaction notifications.

**Example:**
```java
public void pushTransactionNotification(Long userId, Long transactionId, String status) {
    if (!preferenceService.isEnabled(userId, NotificationType.TRANSACTION_UPDATE)) {
        return;
    }

    Notification notification = notificationService.createNotification(
        userId,
        NotificationType.TRANSACTION_UPDATE,
        "Transaction Update",
        "Transaction status: " + status,
        transactionId,
        "transaction"
    );

    pushNotification(userId, notification);
}
```
Source: project pattern based on existing `NotificationPushService`

### Pattern 3: Query Invalidation After Preference Mutation

**What:** Invalidate preference queries immediately after toggle mutations rather than manually syncing every consumer.

**When to use:** Notification settings forms or quick toggles.

**Example:**
```tsx
const queryClient = useQueryClient()

const mutation = useMutation({
  mutationFn: notificationApi.updatePreferences,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['notification-preferences'] })
  },
})
```
Source: TanStack Query query invalidation guide - https://tanstack.com/query/latest/docs/framework/react/guides/query-invalidation

### Anti-Patterns to Avoid

- **Local-only notification settings:** A Zustand or localStorage toggle would not affect backend notification creation, so server and UI would diverge.
- **Hard-coded notification click routes by title text:** Use `type`, `referenceType`, and `referenceId`; notification copy is not a contract.
- **Adding a second chat creation path:** Reuse `POST /api/conversations`; it is already idempotent for `listingId + buyerId`.
- **Relying on only active-conversation subscriptions:** This drops unread state updates for inactive conversations.

## API and Data Model Impact

### Recommended Backend Additions

| Area | Recommendation |
|------|----------------|
| Preferences table | Add `notification_preferences` keyed by `user_id` with booleans for `new_message_enabled`, `item_sold_enabled`, `transaction_update_enabled` |
| Preference REST API | Add `GET /api/notifications/preferences` and `PATCH /api/notifications/preferences` |
| Reference normalization | Normalize `referenceType` writes to lowercase canonical values: `conversation`, `listing`, `transaction` |
| Notification creation gate | Centralize preference checks in notification service/push service so all producers respect settings |

### Recommended Frontend Additions

| Area | Recommendation |
|------|----------------|
| Notification action resolver | Add a helper that maps a notification to a route or conversation bootstrap action |
| Messages page bootstrap | Support `conversation` query param and fetch target conversation when needed |
| Dropdown hydration | Load recent persisted notifications for dropdown, not only unread count |
| Quick settings UI | Add a compact settings panel/modal from dropdown and a fuller section on notifications page if needed |

### Existing Data Model Observations

- `Notification` already has enough linkage for action routing: `referenceId`, `referenceType`, `type`
- `Conversation` already supports seller/buyer unread counts and idempotent creation
- `ListingDetail` already exposes seller summary; `Listing` does not

## Likely Files To Touch

### Frontend

- `frontend/src/pages/NotificationsPage.tsx`
- `frontend/src/components/notifications/NotificationList.tsx`
- `frontend/src/components/notifications/NotificationDropdown.tsx`
- `frontend/src/components/notifications/NotificationItem.tsx`
- `frontend/src/components/notifications/NotificationBell.tsx`
- `frontend/src/api/notificationApi.ts`
- `frontend/src/types/notification.ts`
- `frontend/src/stores/notificationStore.ts`
- `frontend/src/pages/MessagesPage.tsx`
- `frontend/src/components/chat/ConversationList.tsx`
- `frontend/src/components/chat/ChatView.tsx`
- `frontend/src/hooks/useChat.ts`
- `frontend/src/stores/chatStore.ts`
- `frontend/src/api/chatApi.ts`
- `frontend/src/pages/ListingDetailPage.tsx`
- `frontend/src/tests/app-shell.test.tsx`
- likely new frontend tests for notifications/messages flows

### Backend

- `backend/src/main/java/com/tradingplatform/notification/controller/NotificationController.java`
- `backend/src/main/java/com/tradingplatform/notification/service/NotificationService.java`
- `backend/src/main/java/com/tradingplatform/notification/service/NotificationPushService.java`
- `backend/src/main/java/com/tradingplatform/notification/entity/Notification.java`
- likely new `NotificationPreference` entity/repository/dto
- `backend/src/main/java/com/tradingplatform/chat/service/ChatService.java`
- possibly transaction/dispute/rating services only for `referenceType` normalization
- Liquibase changelog for preferences table
- notification/chat controller/service tests

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Chat entry deduplication | Custom frontend "find-or-create" logic based on cached conversations only | Existing `POST /api/conversations` idempotent backend contract | Prevents duplicate threads and keeps authorization server-side |
| Deep-link state | Ad hoc globals for target conversation | Router query params | Shareable, reload-safe, testable |
| Settings persistence | LocalStorage-only notification toggles | Server-backed preferences + query invalidation | Backend notification generation must respect settings |
| Notification action parsing | Title/content string parsing | `type + referenceType + referenceId` | Text changes would break behavior |

**Key insight:** The existing backend contracts are already strong enough for chat bootstrap and notification actions. The missing work is mostly normalization, persistence of preferences, and route-aware UI state.

## Common Pitfalls

### Pitfall 1: Clicking a Notification Opens Messages but Not the Right Conversation

**What goes wrong:** User lands on `/messages` with no conversation selected.
**Why it happens:** `MessagesPage` has no deep-link bootstrap and only tracks selection in local state.
**How to avoid:** Make the conversation ID URL-backed and fetch/select it on mount.
**Warning signs:** Notification click handler only calls `navigate('/messages')`.

### Pitfall 2: Notification Preferences Silence Chat Entirely

**What goes wrong:** Disabling message notifications also suppresses message persistence or unread counts.
**Why it happens:** Notification suppression is applied in the wrong layer.
**How to avoid:** Suppress only notification creation/push, never `ChatService.sendMessage(...)` or conversation unread updates.
**Warning signs:** Preference checks appear inside chat persistence methods.

### Pitfall 3: Inactive Conversation Unread Counts Drift

**What goes wrong:** Messages received in background conversations do not increment unread badges until a full refetch.
**Why it happens:** `useChat` currently only appends messages for the active conversation.
**How to avoid:** On any incoming message, update conversation preview/unread metadata even when the conversation is not active.
**Warning signs:** WebSocket handler returns early unless `msg.conversationId === activeConversationId`.

### Pitfall 4: Notification Detail Routing Breaks on Case Differences

**What goes wrong:** Some notifications open correctly and others do nothing.
**Why it happens:** Existing writers use mixed `referenceType` casing: `"conversation"`, `"listing"`, `"TRANSACTION"`.
**How to avoid:** Normalize on write and tolerate legacy variants on read during migration.
**Warning signs:** Frontend resolver compares strict lowercase strings only.

### Pitfall 5: Dropdown and Full Notifications Page Disagree

**What goes wrong:** Bell badge shows unread items, but dropdown is empty or stale.
**Why it happens:** Store hydration is split between `useNotifications()` and `NotificationList` fetches.
**How to avoid:** Define one shared fetch path for recent notifications and unread count.
**Warning signs:** Multiple components fetch pieces of notification state independently.

## Code Examples

Verified patterns from official sources and the current codebase:

### Notification Action Resolver

```ts
import type { Notification } from '@/types/notification'

export function getNotificationAction(notification: Notification) {
  const referenceType = notification.referenceType?.toLowerCase()

  if (referenceType === 'conversation' && notification.referenceId) {
    return { to: `/messages?conversation=${notification.referenceId}` }
  }

  if (referenceType === 'transaction' && notification.referenceId) {
    return { to: `/transactions/${notification.referenceId}` }
  }

  if (referenceType === 'listing' && notification.referenceId) {
    return { to: `/listings/${notification.referenceId}` }
  }

  return null
}
```

### Seller Chat Bootstrap From Listing Detail

```ts
const conversation = await chatApi.createConversation({ listingId })
navigate(`/messages?conversation=${conversation.id}`)
```

### Preference DTO Shape

```ts
export interface NotificationPreferences {
  newMessageEnabled: boolean
  itemSoldEnabled: boolean
  transactionUpdateEnabled: boolean
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Component-local selection for inbox UIs | URL-backed selection/deep links | Standard in modern SPA routing | Better reload behavior and cross-surface navigation |
| Manual fetch + local sync after mutations | Targeted invalidation with TanStack Query | Established in Query v4/v5 era | Lower sync complexity for preferences |
| Notification payloads without stable targets | Stable references and route resolution | Current standard in notification centers | Enables actionability without parsing copy |

**Deprecated/outdated:**
- Treating notification dropdowns as purely ephemeral UI. In modern UX they are expected to hydrate from persisted server history, at least for recent items.

## Open Questions

1. **Should disabled notification categories suppress database records, real-time pushes, or only badge visibility?**
   - What we know: The roadmap says quick notification settings, not merely UI filters.
   - What's unclear: Whether users should still see suppressed events later in notification history.
   - Recommendation: For this phase, suppress creation and push for disabled in-app categories. Do not implement hidden-but-stored behavior unless product explicitly asks for it.

2. **Does "seller chat entry from listings" include browse cards or only listing detail seller info?**
   - What we know: Listing detail already has seller info; listing cards do not expose seller data.
   - What's unclear: Whether Phase 5 should expand browse/listing summary APIs.
   - Recommendation: Lock scope to listing detail unless a separate requirement explicitly expands listing summary payloads.

3. **Should old mixed-case `referenceType` values be migrated?**
   - What we know: Current writers use inconsistent casing.
   - What's unclear: Whether existing rows already contain mixed values in environments that matter.
   - Recommendation: Read case-insensitively now; normalize all new writes. A data migration is optional unless seeded/demo data already depends on it.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Frontend: Vitest 4.1.0, Backend: Spring Boot Test / JUnit 5 |
| Config file | `frontend/vitest.config.ts` |
| Quick run command | `npm test -- --run app-shell` or targeted `vitest run` file; `mvn -Dtest=NotificationControllerTest,ChatControllerTest,ChatServiceTest test` |
| Full suite command | `npm test` and `mvn test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| NOTF-05 | Notification click opens correct target | frontend component/integration | `npm test -- notification-actions.test.tsx` | ❌ Wave 0 |
| NOTF-06 | User can load and update preferences | backend controller + frontend component | `mvn -Dtest=NotificationControllerTest test` and `npm test -- notification-settings.test.tsx` | backend partial / frontend ❌ |
| NOTF-07 | Disabled categories suppress future notifications | backend service | `mvn -Dtest=NotificationServiceTest,NotificationPushServiceTest test` | partial, needs expansion |
| CHAT-06 | Listing detail can create/reuse seller conversation | frontend integration + backend controller | `mvn -Dtest=ChatControllerTest,ChatServiceTest test` and `npm test -- listing-chat-entry.test.tsx` | backend partial / frontend ❌ |
| CHAT-07 | `/messages?conversation=` selects target conversation | frontend integration | `npm test -- messages-page-routing.test.tsx` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** targeted `vitest run` file plus targeted `mvn -Dtest=... test`
- **Per wave merge:** `npm test` and focused backend notification/chat suite
- **Phase gate:** Full frontend and backend suites green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `frontend/src/tests/notification-actions.test.tsx` — notification click-to-route behavior
- [ ] `frontend/src/tests/messages-page-routing.test.tsx` — query-param conversation bootstrap
- [ ] `frontend/src/tests/listing-chat-entry.test.tsx` — seller info to conversation flow
- [ ] backend notification preference controller/service tests — no preference coverage exists yet
- [ ] backend tests for mixed-case `referenceType` normalization or tolerant reads

## Sources

### Primary (HIGH confidence)

- React Router docs - `useSearchParams` API: https://reactrouter.com/api/hooks/useSearchParams
- TanStack Query docs - query invalidation guide: https://tanstack.com/query/latest/docs/framework/react/guides/query-invalidation
- Spring Framework reference - STOMP/WebSocket user destinations and `SimpMessagingTemplate`: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/user-destination.html
- npm registry - package version verification for `react-router-dom`, `@tanstack/react-query`, `zustand`, `date-fns`

### Secondary (MEDIUM confidence)

- Codebase inspection of current frontend/backend notification and chat flows

### Tertiary (LOW confidence)

- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - existing repo stack plus npm-registry verification
- Architecture: MEDIUM - strong local evidence, but preference model is new and requires product scope lock
- Pitfalls: HIGH - directly grounded in current code paths and state flow gaps

**Research date:** 2026-03-22
**Valid until:** 2026-04-21
