# Phase 6: Chat Presence Reliability, Multi-Conversation Seller Status Sync, Responsive Message Layout, and Redis-Backed Realtime Optimization - Research

**Researched:** 2026-03-24
**Domain:** Distributed chat presence, WebSocket/STOMP scaling, responsive messaging UI
**Confidence:** HIGH

## Summary

This phase is an optimization and reliability pass on top of an existing chat system, not a new chat build. The repo already has persistence-first messaging in MySQL, STOMP over SockJS, REST fallback sends, unread counts on `Conversation`, and an in-memory `PresenceService`. The main planning problem is to remove single-node assumptions without moving correctness into Redis. Message durability, unread truth, and conversation ordering should remain database-backed. Redis should be used only for ephemeral distributed state: session presence, transition events, and cross-node realtime fan-out.

The current frontend also has the right primitives but the wrong state boundary. Presence is tracked per mounted conversation view instead of per seller, transport state is mixed into seller status, and the active thread refreshes by polling every 10 seconds regardless of connection health. Planning should therefore center on a shared seller-presence store keyed by `otherUserId`, connection-aware fallback refresh, and duplicate-safe event application so the same seller stays synchronized across all rows and the active header.

**Primary recommendation:** Keep MySQL as source of truth, add Redis-backed ephemeral presence and pub/sub fan-out on the backend, and move frontend presence to a seller-level shared store that renders transport state separately from seller availability.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4.2 (repo standard) | Backend runtime and messaging integration | Already in repo; keeps Phase 6 incremental and compatible with existing WebSocket, JPA, Redis, and scheduling code |
| Spring WebSocket + STOMP | Boot-managed | Realtime chat transport | Already implemented; Spring officially supports STOMP endpoints, user destinations, broker relay/simple broker config |
| Spring Data Redis | Boot-managed | Redis access for presence/session state and pub/sub | Already in repo; standard Spring integration point for Redis-backed ephemeral state |
| Redis | 7.x (project standard) | Distributed presence/session state and pub/sub fan-out | Mandated by project; appropriate for ephemeral cross-node coordination |
| MySQL | 8.x (project standard) | Durable source of truth for messages, unread counts, conversation ordering | Already the authoritative persistence layer; avoids losing correctness to at-most-once pub/sub |
| React | 19.2.4 | Frontend UI | Existing app runtime |
| Zustand | 5.0.12 | Shared client-side chat/presence state | Already used for chat state; best fit for shared seller-level presence cache |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@stomp/stompjs` | 7.3.0 | STOMP client with reconnect and heartbeat support | Keep existing transport; simplify around its built-in connection lifecycle |
| `sockjs-client` | 1.6.1 | Transport fallback for browser/network compatibility | Keep because the current backend endpoint exposes SockJS |
| `@tanstack/react-query` | 5.95.2 current, 5.91.3 in repo | Connection-aware polling fallback and refresh orchestration | Use for fallback refresh/invalidation if Phase 6 touches fetch orchestration beyond the Zustand store |
| `date-fns` | 4.1.0 in repo | Relative time formatting for `last seen` UI | Use if current string formatting moves client-side |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Redis pub/sub for ephemeral fan-out | Full STOMP broker relay | Broker relay is the more scalable messaging architecture, but it is a larger infrastructure shift than this phase requires |
| Zustand shared presence store | TanStack Query-only presence polling | Query-only polling simplifies some sync logic but does not model push-driven seller presence as cleanly |
| Existing STOMP transport | SSE for presence only | SSE is fine for one-way updates, but Phase 6 already has bidirectional chat over STOMP |

**Installation:**
```bash
# no new frontend packages required
# backend already includes spring-boot-starter-data-redis and spring-session-data-redis
```

**Version verification:** Verified from repo manifests and package registry on 2026-03-24.
```bash
npm view @stomp/stompjs version
npm view sockjs-client version
npm view react version
npm view zustand version
npm view @tanstack/react-query version
```

## Architecture Patterns

### Recommended Project Structure
```text
backend/src/main/java/com/tradingplatform/chat/
|-- controller/              # WebSocket + REST endpoints
|-- dto/                     # Presence, transport, and sync payloads
|-- service/                 # Chat persistence, presence state, fan-out orchestration
|-- redis/                   # Redis pub/sub channels, payloads, listeners, key helpers
`-- repository/              # Durable MySQL queries only

frontend/src/
|-- components/chat/         # Responsive list/thread/composer/bubble UI
|-- hooks/                   # useWebSocket, useChat, useSellerPresence
|-- stores/                  # chatStore + sellerPresenceStore
`-- pages/                   # responsive /messages shell
```

### Pattern 1: Persistence-First, Ephemeral-Fan-Out
**What:** Persist messages and unread-count changes in MySQL first, then publish an ephemeral realtime event through Redis so every app node can fan it out to local WebSocket sessions.

**When to use:** All cross-node message preview, unread badge, and presence transition updates.

**Example:**
```typescript
// Source: project pattern from backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java
// Persist first, then emit an ephemeral event.
const persisted = saveMessageToMySql(...)
publishRedis("chat.message", {
  conversationId: persisted.conversationId,
  messageId: persisted.id,
  recipientUserId,
  senderUserId,
})
```

### Pattern 2: Seller-Level Presence, Conversation-Level Unread
**What:** Key presence by seller/user ID, not by conversation ID. Key unread counts and message previews by conversation ID.

**When to use:** Everywhere the same seller can appear in multiple listing threads.

**Example:**
```typescript
// Source: local Phase 6 UI contract + existing Conversation.otherUserId model
type SellerPresenceState = Record<number, {
  status: 'online' | 'offline' | 'stale'
  lastSeenText: string
  updatedAt: number
}>

// Conversation rows derive presence from otherUserId.
const sellerPresence = presenceByUserId[conversation.otherUserId]
```

### Pattern 3: Transport State Is Separate From Presence State
**What:** The websocket connection lifecycle is app transport state. Seller presence is business state about another user. They must not overwrite each other.

**When to use:** Header pills, row badges, reconnect banners, fallback refresh logic.

**Example:**
```typescript
// Source: Phase 6 UI-SPEC + existing useWebSocket connectionState
const transport = connectionState // connected | connecting | reconnecting | disconnected
const seller = getSellerPresence(otherUserId) // online | offline | stale

// Render independently.
```

### Pattern 4: Connection-Aware Polling Fallback
**What:** Poll only when the transport is degraded, or immediately after reconnect, instead of polling every active thread continuously.

**When to use:** Conversation summaries, message lists, and presence rehydration after reconnect.

**Example:**
```typescript
// Source: project pattern from frontend/src/hooks/useChat.ts, but scoped to degraded state only
const fallbackEnabled = connectionState !== 'connected'
const intervalMs = fallbackEnabled ? 10000 : false
```

### Pattern 5: Idempotent Event Application in the Client Store
**What:** Apply realtime events by message ID / conversation ID / seller ID and ignore duplicates.

**When to use:** Redis-backed fan-out, reconnect replay, REST fallback send, and cross-tab multi-session delivery.

**Example:**
```typescript
// Source: recommended Phase 6 store pattern
if (seenMessageIds.has(message.id)) return
seenMessageIds.add(message.id)
addMessage(message)
syncConversationPreview(message)
```

### Anti-Patterns to Avoid
- **In-memory presence as the source of truth:** `PresenceService` currently uses `ConcurrentHashMap`, which breaks the moment multiple backend instances exist.
- **Per-conversation presence subscriptions as state ownership:** this causes the header and repeated seller rows to disagree.
- **Using Redis pub/sub for durable chat correctness:** Redis pub/sub is not durable; keep chat correctness in MySQL.
- **Always-on thread polling even when connected:** wastes requests and still does not solve cross-row consistency cleanly.
- **Treating disconnect as seller offline immediately:** UI-SPEC explicitly forbids this; preserve last-known presence for 30 seconds.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Distributed presence | Multi-node `ConcurrentHashMap` semantics via app memory | Redis keys + TTL + pub/sub + Spring Data Redis | In-memory maps cannot survive multi-node routing or restarts |
| Durable message delivery | Redis-only chat transport | Existing MySQL persistence-first flow | Redis pub/sub has at-most-once delivery semantics |
| Multi-row seller sync | Independent `useState` per row/view | Shared presence store keyed by seller ID | Prevents divergent state between rows and active header |
| Reconnect UX | Ad-hoc component-local reconnect logic | Centralized `useWebSocket` transport state + shared fallback refresh policy | Keeps reconnect behavior predictable |
| Responsive chat routing | Separate desktop and mobile data models | One `/messages` route with layout switching only | Avoids double-fetching and duplicate state trees |

**Key insight:** Redis is the right place for ephemeral coordination, not business truth. Phase 6 should make realtime feel better without moving persistence, unread truth, or ordering away from MySQL.

## Common Pitfalls

### Pitfall 1: Redis Pub/Sub Misused As Reliable Delivery
**What goes wrong:** Planning assumes Redis pub/sub can guarantee delivery of presence or message events.
**Why it happens:** Redis pub/sub is fast and simple, so it looks like a message bus.
**How to avoid:** Use pub/sub only for transient fan-out. Persist messages and unread changes first, then publish an event that can be safely dropped because REST/MySQL remains authoritative.
**Warning signs:** Missing updates after node restart, attempts to replay pub/sub, or designs that cannot recover without the pub/sub event.

### Pitfall 2: Seller Presence Keyed By Conversation
**What goes wrong:** The same seller appears online in one row and stale in another.
**Why it happens:** Presence subscriptions and state are owned by views instead of a shared seller cache.
**How to avoid:** Normalize presence by `otherUserId` and derive every row/header from that shared state.
**Warning signs:** Duplicate subscriptions to `/topic/presence.{id}` and local `useState` in row/header hooks.

### Pitfall 3: Transport Disconnect Forces Offline Seller UI
**What goes wrong:** Sellers flash offline during reconnects even though their true presence is unknown.
**Why it happens:** Transport state is incorrectly mapped to seller presence.
**How to avoid:** Preserve last known seller state for 30 seconds and render transport banners separately.
**Warning signs:** Presence pill tied directly to `connectionState`.

### Pitfall 4: Duplicate Events Reorder Rows Or Duplicate Messages
**What goes wrong:** Redis fan-out plus REST fallback causes the same message preview or message body to apply twice.
**Why it happens:** Client store updates are append-only and not idempotent.
**How to avoid:** Dedupe by `message.id`, upsert conversations by `conversation.id`, and only reorder when `lastMessageAt` changes.
**Warning signs:** Row jumps without new timestamps or repeated message bubbles after reconnect.

### Pitfall 5: Polling Still Owns Correctness After Adding Redis
**What goes wrong:** The app adds Redis fan-out but leaves the UX dependent on coarse polling.
**Why it happens:** Polling is already present and easy to keep.
**How to avoid:** Move polling to degraded mode only, and trigger explicit rehydrate passes on reconnect.
**Warning signs:** Active thread still polls every 10 seconds while connected.

## Code Examples

Verified patterns from official sources and repo-aligned usage:

### Spring STOMP Simple Broker Configuration
```java
// Source: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-simple-broker.html
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
}
```

### Spring Broker Relay Switch Point
```java
// Source: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-broker-relay.html
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableStompBrokerRelay("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
}
```

### Redis Pub/Sub Listener Container
```java
// Source: https://docs.spring.io/spring-data/redis/reference/redis/pubsub.html
RedisMessageListenerContainer container = new RedisMessageListenerContainer();
container.setConnectionFactory(redisConnectionFactory);
container.addMessageListener(messageListener, new ChannelTopic("chat.presence"));
```

### STOMP.js Reconnect And Heartbeats
```typescript
// Source: https://stomp-js.github.io/guide/stompjs/using-stompjs-v5.html
const client = new Client({
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
})
```

### Repo-Aligned Presence Transition Contract
```typescript
// Source: local Phase 6 UI-SPEC + existing frontend connection states
if (transport !== 'connected' && Date.now() - presence.updatedAt < 30_000) {
  return presence.status === 'online' ? 'Seller online' : presence.lastSeenText
}

if (transport !== 'connected') {
  return 'Status updating'
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Per-instance in-memory presence | Redis-backed ephemeral presence/session state + pub/sub fan-out | Use now for Phase 6 | Removes single-node limitation |
| Per-view presence state | Shared seller-level presence cache | Use now for Phase 6 | Synchronizes repeated seller rows and active header |
| Always-on thread polling | Connection-aware degraded polling + reconnect rehydrate | Use now for Phase 6 | Reduces network noise and improves correctness |
| Message persistence plus local WebSocket push only | Message persistence plus cross-node Redis event fan-out | Use now for Phase 6 | Keeps realtime updates working across app nodes |

**Deprecated/outdated:**
- In-memory presence tracking in [`PresenceService.java`](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/chat/service/PresenceService.java): acceptable for Phase 3, no longer acceptable for Phase 6 goals.
- Per-hook presence ownership in [`useConversationPresence.ts`](/d:/Java/Projects/realTimeTrading/frontend/src/hooks/useConversationPresence.ts): replace with shared seller-level state.
- Always-on refresh interval in [`ChatView.tsx`](/d:/Java/Projects/realTimeTrading/frontend/src/components/chat/ChatView.tsx): downgrade to degraded-mode fallback only.

## Open Questions

1. **Is Phase 6 expected to support multiple backend instances immediately, or just prepare the codepath for it?**
   - What we know: the phase title explicitly calls for Redis-backed realtime optimization, and current in-memory presence is a known v1 shortcut.
   - What's unclear: whether deployment already runs more than one app node.
   - Recommendation: plan for multi-node correctness now; it is the main reason to introduce Redis-backed presence.

2. **Should frontend fallback refresh move fully into TanStack Query or stay store-driven?**
   - What we know: project standard says TanStack Query for server state, but current chat code is mostly Zustand + imperative API calls.
   - What's unclear: whether the team wants a local refactor of chat fetching patterns during this phase.
   - Recommendation: keep Phase 6 incremental. Add shared Zustand presence state and targeted fallback refresh first; avoid a broad chat Query rewrite unless planning discovers adjacent cleanup is cheap.

3. **Is seller-online notification deduplication per conversation still desired once seller status sync is seller-level?**
   - What we know: `NotificationPushService` currently emits `SELLER_ONLINE` notifications per conversation.
   - What's unclear: whether that behavior should remain or collapse to one notification per seller session.
   - Recommendation: keep notification semantics out of scope unless they block UX consistency; limit Phase 6 to presence rendering and realtime sync.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Spring Boot Test + JUnit 5, Vitest 4.1.0 |
| Config file | [`frontend/vitest.config.ts`](/d:/Java/Projects/realTimeTrading/frontend/vitest.config.ts), [`backend/src/test/resources/application-test.yml`](/d:/Java/Projects/realTimeTrading/backend/src/test/resources/application-test.yml) |
| Quick run command | `cd backend && mvn -q -Dtest=ChatWebSocketControllerTest,ChatServiceTest test` and `cd frontend && npm test -- --run src/tests/messages-page-routing.test.tsx` |
| Full suite command | `cd backend && mvn test` and `cd frontend && npm test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| P6-01 | Presence survives reconnects and only transitions seller offline after timeout/stale window | backend unit/integration | `cd backend && mvn -q -Dtest=PresenceServiceRedisTest,ChatWebSocketControllerTest test` | NO - Wave 0 |
| P6-02 | Same seller presence syncs across multiple conversation rows and active header | frontend component/store | `cd frontend && npm test -- --run src/tests/chat-presence-sync.test.tsx` | NO - Wave 0 |
| P6-03 | Realtime message preview/unread sync stays duplicate-safe under fallback and reconnect | frontend store/component | `cd frontend && npm test -- --run src/tests/chat-realtime-fallback.test.tsx` | NO - Wave 0 |
| P6-04 | `/messages` route switches correctly between desktop two-pane and mobile single-pane shell | frontend component | `cd frontend && npm test -- --run src/tests/messages-responsive-layout.test.tsx` | NO - Wave 0 |
| P6-05 | Redis-backed pub/sub fan-out reaches local websocket delivery without moving persistence out of MySQL | backend integration | `cd backend && mvn -q -Dtest=RedisChatFanoutIntegrationTest test` | NO - Wave 0 |

### Sampling Rate
- **Per task commit:** `cd backend && mvn -q -Dtest=ChatWebSocketControllerTest,ChatServiceTest test` and `cd frontend && npm test -- --run src/tests/messages-page-routing.test.tsx src/tests/chat-presence-sync.test.tsx`
- **Per wave merge:** `cd backend && mvn test` and `cd frontend && npm test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java` - distributed presence TTL, session accounting, stale transition behavior
- [ ] `backend/src/test/java/com/tradingplatform/chat/integration/RedisChatFanoutIntegrationTest.java` - Redis pub/sub to WebSocket fan-out path
- [ ] `frontend/src/tests/chat-presence-sync.test.tsx` - repeated seller rows and active header share one presence source
- [ ] `frontend/src/tests/chat-realtime-fallback.test.tsx` - reconnect, degraded polling, duplicate-safe event application
- [ ] `frontend/src/tests/messages-responsive-layout.test.tsx` - mobile single-pane vs desktop two-pane shell

## Sources

### Primary (HIGH confidence)
- Repo code:
  - [`PresenceService.java`](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/chat/service/PresenceService.java)
  - [`ChatWebSocketController.java`](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java)
  - [`WebSocketConfig.java`](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/config/WebSocketConfig.java)
  - [`useWebSocket.ts`](/d:/Java/Projects/realTimeTrading/frontend/src/hooks/useWebSocket.ts)
  - [`useConversationPresence.ts`](/d:/Java/Projects/realTimeTrading/frontend/src/hooks/useConversationPresence.ts)
  - [`ChatView.tsx`](/d:/Java/Projects/realTimeTrading/frontend/src/components/chat/ChatView.tsx)
  - [`MessagesPage.tsx`](/d:/Java/Projects/realTimeTrading/frontend/src/pages/MessagesPage.tsx)
  - [`06-UI-SPEC.md`](/d:/Java/Projects/realTimeTrading/.planning/phases/06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization/06-UI-SPEC.md)
- Spring Framework STOMP simple broker: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-simple-broker.html
- Spring Framework STOMP broker relay: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-broker-relay.html
- Spring Framework user destinations: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/user-destination.html
- Spring Data Redis pub/sub: https://docs.spring.io/spring-data/redis/reference/redis/pubsub.html
- Redis pub/sub semantics: https://redis.io/docs/latest/develop/pubsub/
- STOMP.js reconnect and heartbeat guide: https://stomp-js.github.io/guide/stompjs/using-stompjs-v5.html

### Secondary (MEDIUM confidence)
- npm registry verification for frontend package versions on 2026-03-24

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - driven by existing repo stack plus official Spring/Redis/STOMP docs
- Architecture: HIGH - directly mapped from current implementation gaps and official transport/storage semantics
- Pitfalls: HIGH - most are visible in current code and reinforced by official Redis/Spring behavior

**Research date:** 2026-03-24
**Valid until:** 2026-04-23
