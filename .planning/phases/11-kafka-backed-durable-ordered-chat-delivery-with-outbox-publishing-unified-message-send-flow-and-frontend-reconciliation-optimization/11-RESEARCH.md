---
phase: 11
slug: kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
researched: 2026-03-29
status: complete
---

# Phase 11 Research

## Standard Stack

- Spring Boot 3.x with Spring Data JPA for message and outbox persistence
- MySQL as the durable source of truth for chat messages, delivery lifecycle metadata, and outbox rows
- Spring Kafka for ordered message delivery events keyed by `conversationId`
- Redis retained for presence and typing only
- STOMP/WebSocket retained as the browser delivery channel
- React + Zustand frontend keeps optimistic sends and local dedupe, but replaces ACK-driven full refreshes with targeted reconciliation

## Architecture Patterns

### 1. Transactional Outbox for Chat Send

The backend should stop publishing realtime delivery directly from the request thread after persistence. The send path should:

1. Validate sender and conversation membership
2. Persist the `ChatMessage`
3. Persist an outbox row that describes the delivery event
4. Commit the transaction
5. Return an immediate sender acknowledgment with status `PERSISTED`
6. Let an outbox relay publish the delivery event to Kafka asynchronously

This preserves persistence-first correctness and avoids lying about delivery before any consumer push occurs.

### 2. Kafka-Ordered Delivery by Conversation

Kafka topics should use `conversationId` as the record key so all events for a conversation land in one partition and preserve order for that thread. This is the right ordering boundary for chat here because the UI is conversation-centric, not global-user-feed-centric.

### 3. Realtime Gateway Consumer

A dedicated delivery consumer layer should read Kafka delivery events and push them to online recipients through `SimpMessagingTemplate`. This consumer, not the original request thread, should own:

- recipient websocket push
- delivery-state transition from `PERSISTED` to `DELIVERED` when the event is processed for live delivery
- follow-up notification production if the recipient is offline or not actively connected

### 4. Unified Backend Command Path

`ChatController` and `ChatWebSocketController` should stop owning separate send implementations. Both transports should call one application service or command handler, for example:

- `ChatMessageCommandService.persistMessage(...)`

That service should return a persisted message envelope used by both REST and WebSocket callers.

### 5. Optimistic Reconciliation Instead of Full Refresh

The frontend should keep negative temporary IDs for optimistic messages, but the ack payload should include enough data to reconcile that optimistic entry with the persisted message:

- client message correlation ID
- persisted server message ID
- conversation ID
- persisted timestamp
- status `PERSISTED`

This removes the need for `useChat.ts` to fully reload the active conversation every time `/user/queue/message-ack` fires.

### 6. Cursor-Based Catch-Up on Reconnect

On reconnect, the client should ask for messages after the latest confirmed persisted marker for the active conversation rather than re-fetching whole message pages and full conversation lists on every reconnect.

Good cursor candidates:

- `lastPersistedMessageId`
- or `(conversationId, createdAt, id)` if strict timestamp-plus-tie ordering is needed

For this codebase, `messageId` is simpler and aligns with current MySQL identity-based ordering assumptions.

## Recommended Data Model Changes

### Chat Message Lifecycle

Current lifecycle:
- `SENT`
- `DELIVERED`
- `READ`

Recommended lifecycle:
- `PERSISTED` or rename current initial state from `SENT` to `PERSISTED`
- `DELIVERED`
- `READ`

If the existing `SENT` enum is reused to mean durable acceptance, the API contract should still expose sender ACK semantics clearly as persisted, not delivered.

### Outbox Table

Add an outbox table such as `chat_message_outbox` with:

- `id`
- `message_id`
- `conversation_id`
- `recipient_user_id`
- `event_type`
- `payload`
- `status` (`PENDING`, `PUBLISHED`, `FAILED`)
- `attempt_count`
- `created_at`
- `published_at`

### Message Cursor Support

Add query support for:

- fetch recent messages as today
- fetch messages after `messageId`
- fetch conversation delta metadata after a reconnect marker

## Don’t Hand-Roll

- Do not invent a custom broker or queue in Redis for durable delivery
- Do not keep direct request-thread `SimpMessagingTemplate` delivery as the main send model
- Do not use the current sender ACK as proof of recipient delivery
- Do not refresh full conversation state on every successful send

## Common Pitfalls

- Publishing to Kafka before the DB transaction commits creates ghost deliveries and broken reconciliation
- Keying Kafka by recipient instead of conversation breaks in-conversation ordering
- Reusing one event stream for presence/typing and durable delivery increases noise and complicates ephemeral behavior
- Updating unread counts or conversation preview twice during optimistic send and persisted event processing can reintroduce duplicate side effects
- Leaving REST and WebSocket sends on separate code paths will recreate drift after the refactor

## Migration Strategy

1. Introduce a unified message persistence service first
2. Add outbox persistence without changing client transport behavior
3. Add Kafka publication and consumer delivery
4. Move REST sends onto the unified path
5. Change sender ACK semantics to `PERSISTED`
6. Update the frontend to reconcile optimistic messages against persisted ACK payloads
7. Add reconnect catch-up endpoints or cursor parameters
8. Remove the old direct-delivery hot path once tests prove equivalence

## Code Examples

### Unified Send Shape

```java
public PersistedChatMessage persistMessage(SendChatMessageCommand command)
```

This service should own validation, DB writes, conversation metadata updates, and outbox row creation.

### Kafka Record Key

```text
topic: chat.message.persisted
key: conversationId
```

### Sender Ack Contract

```json
{
  "clientMessageId": "temp-123",
  "messageId": 4821,
  "conversationId": 77,
  "status": "PERSISTED",
  "createdAt": "2026-03-29T13:30:00Z"
}
```

## Validation Architecture

Phase 11 needs automated proof at four layers:

1. Backend unit tests for outbox creation and unified send service semantics
2. Backend integration tests for transactional outbox relay and Kafka ordering by `conversationId`
3. WebSocket/consumer integration tests for async recipient delivery and delivery-state updates
4. Frontend tests for optimistic reconciliation and reconnect catch-up without full ACK refreshes

Recommended targeted suites:

- backend: unified send service + outbox repository tests
- backend: Kafka consumer ordering and delivery integration
- frontend: `useChat` ack reconciliation test
- frontend: reconnect catch-up test using persisted cursor markers

Manual verification should focus on:

- two-browser send/receive with delayed async delivery
- reconnect while messages arrive in the background
- confirming typing and presence still flow through Redis without Kafka dependency

## Plan Implications

The phase should likely split into:

- backend schema and unified send/outbox foundation
- Kafka relay and async delivery consumer path
- frontend ACK reconciliation and reconnect catch-up
- verification gate covering ordering, durability, and UX parity

---

*Phase: 11-kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization*
*Researched: 2026-03-29 via inline fallback after subagent timeout*
