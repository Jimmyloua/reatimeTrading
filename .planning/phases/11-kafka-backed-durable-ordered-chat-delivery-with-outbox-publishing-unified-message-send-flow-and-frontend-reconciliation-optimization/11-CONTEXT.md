# Phase 11: Kafka-backed durable ordered chat delivery with outbox publishing, unified message send flow, and frontend reconciliation optimization - Context

**Gathered:** 2026-03-29
**Status:** Ready for planning
**Source:** User-provided architecture direction during Phase 11 kickoff

<domain>
## Phase Boundary

This phase upgrades chat delivery from the current request-thread-driven Redis/WebSocket fan-out into a durable, ordered, asynchronous delivery pipeline.

The phase should:

- Keep Redis for presence and typing because those signals are ephemeral and latency-sensitive.
- Move message delivery onto Kafka with ordering keyed by `conversationId`.
- Persist the chat message and an outbox event in one transaction.
- Acknowledge the sender immediately as `PERSISTED` after the durable write succeeds.
- Publish to the message bus asynchronously from the outbox path.
- Let realtime delivery consumers push to online recipients and update delivery state separately.
- Unify REST and WebSocket sends so both use the same backend command path and delivery model.
- Replace frontend ACK-driven full refreshes with targeted optimistic-message reconciliation and cursor-based catch-up after reconnect.

</domain>

<decisions>
## Implementation Decisions

### Delivery Architecture
- Redis remains the transport for presence and typing only; do not migrate those signals to Kafka.
- Kafka becomes the durable ordered delivery stream for chat messages.
- Kafka partitioning must preserve per-conversation ordering by keying events with `conversationId`.
- Message persistence and outbox event creation must happen in the same transaction.
- Sender acknowledgment semantics must change from "DELIVERED" to "PERSISTED" at initial acceptance time.
- Recipient delivery and delivery-status updates must occur asynchronously after durable persistence.

### Backend Flow
- REST and WebSocket message sends must converge on one backend command path instead of maintaining separate delivery implementations.
- The current synchronous request hot path should be narrowed so notification or fan-out work does not extend sender-facing latency unnecessarily.
- Delivery status should be modeled explicitly enough to distinguish persistence, delivery, and read transitions.

### Frontend Flow
- The frontend should keep optimistic sends.
- ACK handling should reconcile the optimistic message with the persisted server message rather than refreshing the full active conversation.
- Reconnect behavior should use cursor-based catch-up instead of broad conversation/message refetches where possible.

### Constraints
- Preserve persistence-first correctness.
- Do not replace working Redis presence/typing behavior with a more complex durable pipeline.
- Avoid introducing silent fallbacks or fake-success paths; failures should remain observable.

### the agent's Discretion
- Exact outbox schema, Kafka topic naming, consumer topology, and delivery-status field design.
- Whether the delivery publisher runs inline post-commit, via scheduled relay, or another explicit outbox-drain mechanism.
- Exact frontend cursor format and reconciliation strategy, as long as it removes ACK-driven full refreshes.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Current Realtime Backend
- `.planning/phases/03-real-time-communication/03-03-SUMMARY.md` - Original persistence-first WebSocket design and notification push intent
- `backend/src/main/java/com/tradingplatform/chat/controller/ChatWebSocketController.java` - Current WebSocket send, typing, heartbeat, and ACK flow
- `backend/src/main/java/com/tradingplatform/chat/controller/ChatController.java` - Current REST send path that must be unified
- `backend/src/main/java/com/tradingplatform/chat/service/ChatService.java` - Current message persistence and conversation metadata updates
- `backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventPublisher.java` - Current Redis event publication contract
- `backend/src/main/java/com/tradingplatform/chat/redis/RedisChatEventSubscriber.java` - Current Redis-to-WebSocket delivery bridge
- `backend/src/main/java/com/tradingplatform/notification/service/NotificationPushService.java` - Current synchronous notification push coupling on send

### Current Realtime Frontend
- `.planning/phases/06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization/06-02-SUMMARY.md` - Duplicate-safe reconnect and reconciliation patterns already established
- `frontend/src/hooks/useChat.ts` - Current optimistic send, ACK refresh, reconnect rehydrate, and degraded polling behavior
- `frontend/src/hooks/useWebSocket.ts` - Current STOMP connection lifecycle and heartbeat behavior
- `frontend/src/stores/chatStore.ts` - Current dedupe and conversation preview state model
- `frontend/src/types/chat.ts` - Current message and conversation contracts

### Planning State
- `.planning/ROADMAP.md` - Phase ordering and dependency expectations
- `.planning/STATE.md` - Current focus, project history, and established decisions
- `.planning/REQUIREMENTS.md` - Existing product requirements and reopened gap-closure phases

</canonical_refs>

<specifics>
## Specific Ideas

- Use Kafka because delivery ordering and scalable consumer fan-out are now more important than keeping delivery fully in-process.
- Keep the sender experience fast by shrinking the synchronous work to durable write plus lightweight acknowledgment.
- Make the architecture explicit enough that future multi-node scaling does not depend on direct `SimpMessagingTemplate` delivery from request handlers.

</specifics>

<deferred>
## Deferred Ideas

- Broader chat feature additions unrelated to delivery architecture.
- Replacing Redis presence/typing with a durable event stream.
- Any v2 escrow or unrelated marketplace roadmap work.

</deferred>

---

*Phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization*
*Context gathered: 2026-03-29 via user-provided architecture direction*
