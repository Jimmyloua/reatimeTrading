# Phase 3: Real-Time Communication - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can communicate in real-time about items through chat, and receive instant notifications for messages and important events. This phase delivers WebSocket-based chat with message persistence, typing indicators, online presence, and a notification system. Payment processing and structured trading actions (offers, accept/reject) are Phase 4.

</domain>

<decisions>
## Implementation Decisions

### Chat Initiation & Context
- **D-01:** Users can initiate chats from both item detail page ("Contact Seller" button) AND directly from user profiles (direct messaging)
- **D-02:** Each item creates a separate conversation thread with the same user (not merged into single thread)
- **D-03:** Item context shown as clickable link in chat header (no price snapshot or title snapshot)
- **D-04:** Clicking item link opens item detail in new tab

### Message Retention & Status
- **D-05:** Messages auto-deleted after 90 days (time-limited retention)
- **D-06:** Users see read receipts (message status: sent, delivered, read)
- **D-07:** Image sharing supported in chat (images only for v1, no documents/files)
- **D-08:** Max image size: 5MB per image (consistent with avatar limit from Phase 1)

### Typing & Presence
- **D-09:** Typing indicator shown ("User is typing...") in active conversations
- **D-10:** Online/offline presence shown per user ("online" or "last seen X ago")
- **D-11:** Presence status visible in chat header and conversation list

### Notification Delivery
- **D-12:** Real-time WebSocket push only for v1 (no email fallback, no browser push notifications)
- **D-13:** Notification history limited to 50 unread, retained for 30 days
- **D-14:** Notification types: new messages, item sold, transaction updates, system announcements, payment status changes
- **D-15:** Users can mark notifications as read (supports NOTF-04)

### Chat UI
- **D-16:** Dedicated /messages page with conversation list and chat view (not floating widget)
- **D-17:** Conversations ordered by most recent message (most recent at top)
- **D-18:** Unread message count shown on conversation list items

### Claude's Discretion
- Exact WebSocket reconnection strategy (exponential backoff, max retries)
- Message pagination for chat history (number of messages per page)
- Typing indicator debounce timing (how long after stop typing)
- Presence heartbeat interval (how often to check online status)
- Notification grouping/batching strategy
- Image upload flow (drag-drop, click to upload, preview before send)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — CHAT-01 to CHAT-05, NOTF-01 to NOTF-04
- `.planning/ROADMAP.md` — Phase 3 details, success criteria, architecture notes
- `.planning/PROJECT.md` — Tech stack mandates (Spring Boot 3.5.x, JDK 21, MySQL 8, Redis 7, Kafka 4)

### Prior Phase Context
- `.planning/phases/01-foundation-and-user-management/01-CONTEXT.md` — User entity, profile requirements (D-06: profile required before interactions)

### Architecture Notes
- **ROADMAP.md Critical Note:** Messages must be written to database before WebSocket delivery to prevent message loss on server restart
- **PROJECT.md Stack:** Spring WebSocket, STOMP, Apache Kafka 4, @stomp/stompjs, sockjs-client mandated

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **User entity** (`backend/src/main/java/com/tradingplatform/user/User.java`) — Chat references users for sender/receiver
- **Listing entity** (`backend/src/main/java/com/tradingplatform/listing/entity/Listing.java`) — Chat references items for context
- **Redis configuration** — Already configured for sessions; extend for WebSocket message distribution (pub/sub)
- **Zustand auth store** (`frontend/src/stores/authStore.ts`) — Extend for WebSocket connection state
- **API pattern** (`frontend/src/api/`) — Established Axios pattern; add WebSocket client setup

### Established Patterns
- **Entity pattern:** JPA with Lombok (@Getter, @Setter, @Builder, auditing listeners)
- **Repository pattern:** Spring Data JPA with derived queries
- **Service layer:** @Service classes with @Transactional methods
- **DTO mapping:** MapStruct for entity-DTO conversion
- **Migrations:** Liquibase for schema changes
- **Frontend state:** Zustand for client state, TanStack Query for server state

### Integration Points
- **Chat entity** will need foreign keys to User (sender, receiver) and Listing (item context)
- **Notification entity** will need foreign key to User (recipient)
- **WebSocket config** will integrate with existing Spring Security (JWT authentication on connect)
- **Redis pub/sub** will use existing Redis connection for message distribution across instances
- **Kafka** will be new infrastructure for message persistence and event streaming

</code_context>

<specifics>
## Specific Ideas

- Separate conversation per item keeps trading negotiations organized (not merged into single thread per user)
- Item link in chat header allows quick reference back to the product being discussed
- 90-day retention balances storage costs with useful history for disputes/negotiations
- Images in chat useful for sharing product condition photos, receipts, shipping labels
- Online presence helps set response time expectations

</specifics>

<deferred>
## Deferred Ideas

### Structured Trading Actions (Phase 4)
- "Make offer" button in chat
- "Accept" / "Reject" / "Counter" offer actions
- "Confirm payment" action
- These require transaction state machine and escrow workflow from Phase 4

### Payment Notifications (Phase 4)
- "Your item is on pay" notification
- "Payment cancelled" notification
- Requires integration with payment/escrow system

### Future Enhancements (v2+)
- Email fallback for offline users
- Browser push notifications
- Document/file sharing in chat
- Message editing and deletion
- Voice/video calling

</deferred>

---

*Phase: 03-real-time-communication*
*Context gathered: 2026-03-22*