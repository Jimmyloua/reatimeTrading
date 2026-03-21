# Phase 3: Real-Time Communication - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-22
**Phase:** 03-real-time-communication
**Areas discussed:** Chat Context & Initiation, Message Retention, Typing & Presence, Notification Delivery, Chat UI

---

## Chat Initiation

| Option | Description | Selected |
|--------|-------------|----------|
| From item page only | "Contact Seller" button on item detail. Chat linked to item. | |
| Direct message from profile | Message any user from their profile. No item context. | |
| Both approaches | Item-initiated AND direct user-to-user messaging. | ✓ |

**User's choice:** Both approaches
**Notes:** Maximum flexibility for users. Allows both item-contextual chats and direct messaging.

---

## Item Context

| Option | Description | Selected |
|--------|-------------|----------|
| Link to item only | Item link in chat header. Clicking opens item in new tab. | ✓ |
| Snapshot price + item info | Snapshot title/price at chat start. Shows "$150 (when discussed)". | |

**User's choice:** Link to item only
**Notes:** Simpler implementation. Price changes on item don't create confusion in chat history.

---

## Conversation Threads

| Option | Description | Selected |
|--------|-------------|----------|
| Single thread per user | One conversation per user pair. All messages in same thread. | |
| Separate thread per item | Each item creates separate conversation with same user. | ✓ |

**User's choice:** Separate thread per item
**Notes:** Keeps trading negotiations organized by item. A user can have multiple concurrent chats with same seller about different items.

---

## Message Retention

| Option | Description | Selected |
|--------|-------------|----------|
| Indefinite | Keep all messages forever. Users can delete their own copy. | |
| Time-limited (30/90 days) | Auto-delete messages after N days. | ✓ |

**User's choice:** Time-limited (90 days)
**Notes:** 90-day retention balances storage costs with useful history for disputes.

---

## Read/Delivery Status

| Option | Description | Selected |
|--------|-------------|----------|
| Yes - show read receipts | Show sent, delivered, read status. | ✓ |
| Delivery status only | Show delivered only, no read status. | |
| No status indicators | No delivery or read status. | |

**User's choice:** Yes - show read receipts
**Notes:** Essential for trading context - users need to know if their message was seen.

---

## File Sharing

| Option | Description | Selected |
|--------|-------------|----------|
| Images only (v1) | Images up to 5MB per image. | ✓ |
| Images + documents | Images, PDFs, documents. | |
| Any file type | All file types. | |

**User's choice:** Images only
**Notes:** Sufficient for product photos, receipts. Documents can be added in future.

---

## Typing Indicators

| Option | Description | Selected |
|--------|-------------|----------|
| Yes - show typing | "User is typing..." indicator. | ✓ |
| No typing indicator | No typing indicator. | |

**User's choice:** Yes - show typing
**Notes:** Improves real-time feel. Standard expectation in modern chat.

---

## Online Presence

| Option | Description | Selected |
|--------|-------------|----------|
| Yes - show presence | Show "online" / "last seen X ago". | ✓ |
| No presence indicators | No online/offline status shown. | |

**User's choice:** Yes - show presence
**Notes:** Helps set expectations for response time.

---

## Notification Delivery

| Option | Description | Selected |
|--------|-------------|----------|
| Real-time only (v1) | WebSocket push when user has app open. | ✓ |
| Real-time + email fallback | WebSocket + email if user offline. | |
| Real-time + browser push | WebSocket + browser push notifications. | |

**User's choice:** Real-time only
**Notes:** Simplest for v1. Email/push can be added later.

---

## Notification History

| Option | Description | Selected |
|--------|-------------|----------|
| Limited history | Last 50 unread, retained 30 days. | ✓ |
| Unlimited history | All notifications ever. | |

**User's choice:** Limited history
**Notes:** Balance between utility and storage.

---

## Notification Types

| Option | Description | Selected |
|--------|-------------|----------|
| New message | Push when new message arrives. | ✓ |
| Item sold | Push when item marked as sold. | ✓ |
| Transaction updates | Push on transaction state changes. | ✓ |
| System announcements | Generic system event notifications. | ✓ |

**User's choice:** All selected
**Notes:** Plus payment status notifications deferred to Phase 4.

---

## Chat UI Pattern

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated messages page | /messages page with conversation list and chat view. | ✓ |
| Floating chat widget | Floating widget in corner, available on any page. | |
| Both page and widget | Full page AND floating widget. | |

**User's choice:** Dedicated messages page
**Notes:** Standard pattern, more screen space for reading messages.

---

## Conversation Ordering

| Option | Description | Selected |
|--------|-------------|----------|
| By most recent message | Most recent message at top. | ✓ |
| Pinned + recent | User can pin conversations. Pinned at top, rest by recency. | |

**User's choice:** By most recent message
**Notes:** Simple, predictable ordering. Pinned can be added later if needed.

---

## Claude's Discretion

- WebSocket reconnection strategy (exponential backoff)
- Message pagination for chat history
- Typing indicator debounce timing
- Presence heartbeat interval
- Notification grouping strategy
- Image upload flow (drag-drop, preview)

## Deferred Ideas

- **Structured trading actions** (make offer, accept, reject, counter, confirm payment) — Phase 4
- **Payment notifications** (pay/cancel pay) — Phase 4
- Email fallback for offline users — v2
- Browser push notifications — v2
- Document/file sharing — v2