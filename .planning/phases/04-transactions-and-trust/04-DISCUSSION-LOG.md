# Phase 4: Transactions and Trust - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-22
**Phase:** 04-transactions-and-trust
**Areas discussed:** Transaction initiation, Transaction status model, Cancellation rules, Dispute handling, Transaction data capture, Transaction history UI, Rating timing & visibility, Rating form, Rating display, Atomicity & financial integrity

---

## Transaction Initiation

| Option | Description | Selected |
|--------|-------------|----------|
| Buyer initiates request | Buyer clicks 'Request to Buy' → Seller accepts/declines | ✓ |
| Seller marks sold directly | Seller directly marks item sold to buyer | |
| Both can initiate from chat | Either party can start from chat, first action wins | |

**User's choice:** Buyer initiates request (Recommended)
**Notes:** Creates explicit handshake before marking sold, gives buyer active role

---

## Post-Acceptance Flow

| Option | Description | Selected |
|--------|-------------|----------|
| Simple 3-step | Request → Accepted → Completed | |
| 5-step delivery tracking | Request → Accepted → Payment Confirmed → Shipped → Completed | |
| Full lifecycle | created → funded → reserved → delivered → confirmed → settled → completed with failure states | ✓ |

**User's choice:** Model the trade lifecycle explicitly
**Notes:** Include failure states: cancelled, expired, disputed, refunded. Full transparency to users.

---

## State Visibility

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit actor-driven transitions | Each state requires explicit action | |
| Simplified user-facing states | Show less detail, track internally | |
| Full transparency | Show full lifecycle to both parties | ✓ |

**User's choice:** Full transparency (Recommended)
**Notes:** Transparency builds trust. Both parties see all states.

---

## State Transitions

| Option | Description | Selected |
|--------|-------------|----------|
| Each party confirms step | Funded: buyer marks, Reserved: seller confirms, etc. | ✓ |
| Minimal confirmation points | Only seller accept and delivery confirmation | |
| Time-based auto-progression | Auto-progress after X days | |

**User's choice:** Each party confirms their step (Recommended)
**Notes:** FUNDED: buyer marks payment sent. RESERVED: seller confirms. DELIVERED: seller marks. CONFIRMED: buyer receives. SETTLED: automatic.

---

## Cancellation Rules

| Option | Description | Selected |
|--------|-------------|----------|
| Mutual cancel before funded | Either party can cancel before funded, mutual agreement after | ✓ |
| Either party anytime | Either can cancel at any time | |
| Locked after funded | Cannot cancel after funded, dispute only | |

**User's choice:** Detailed cancellation moments
**Notes:** Cancelable: before payment, before acceptance, before shipping, missed deadlines, fraud checks. NOT cancelable after: delivery, settlement, final dispute resolution.

---

## Dispute Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Admin-mediated resolution | Admin reviews and resolves | ✓ |
| Automated time-based | Auto-resolve based on deadlines | |
| No dispute handling in v1 | Users work it out via chat | |

**User's choice:** Admin-mediated resolution (Recommended)
**Notes:** Full dispute workflow: open → evidence submission → review → resolution. Dispute statuses: OPEN, UNDER_REVIEW, WAITING_BUYER_EVIDENCE, WAITING_SELLER_EVIDENCE, RESOLVED_BUYER, RESOLVED_SELLER, PARTIALLY_RESOLVED, ESCALATED, CLOSED.

---

## Transaction Data Capture

| Option | Description | Selected |
|--------|-------------|----------|
| Full transaction record | Core + commercial + cash flow + lifecycle + fulfillment + communication + dispute data | ✓ |
| Essential fields only | Buyer, seller, listing, price, status | |

**User's choice:** Comprehensive data capture
**Notes:** Core data (IDs, status, timestamps), Commercial data (price, fees, taxes, net amounts), Cash flow (payment status, escrow, reserved/settled amounts), Lifecycle timestamps, Fulfillment data, Communication/trust data, Dispute/compliance data.

---

## Transaction History UI

| Option | Description | Selected |
|--------|-------------|----------|
| Profile tabs for purchases/sales | Separate tabs with filters | ✓ |
| Unified activity feed | Single list with filters | |
| Contextual only | No dedicated page | |

**User's choice:** Profile tabs for purchases/sales (Recommended)
**Notes:** Filter by status (active, completed, cancelled)

---

## Rating Timing

| Option | Description | Selected |
|--------|-------------|----------|
| After SETTLED only | Rating only after settlement | ✓ |
| After DELIVERED | Rating after delivery | |
| Anytime after creation | Rating any time | |

**User's choice:** After SETTLED only (Recommended)
**Notes:** Prevents retaliatory ratings during disputes

---

## Rating Visibility

| Option | Description | Selected |
|--------|-------------|----------|
| Blind rating | Both rate before revealing | ✓ |
| Immediate visibility | Ratings visible immediately | |
| Seller first | Seller rates first, then buyer | |

**User's choice:** Blind rating
**Notes:** Ratings revealed after both submit OR after window closes. Prevents retaliation.

---

## Rating Window

| Option | Description | Selected |
|--------|-------------|----------|
| 14-day rating window | 14 days to rate after settlement | ✓ |
| 7-day rating window | 7 days to rate | |
| No expiration | Rate anytime | |

**User's choice:** 14-day rating window (Recommended)
**Notes:** Auto-remind at day 7

---

## Cancelled/Disputed Ratings

| Option | Description | Selected |
|--------|-------------|----------|
| No ratings for cancelled/disputed | Only settled transactions ratable | ✓ |
| Cancelled yes, disputed no | Partial allowance | |
| All outcomes ratable | All transactions ratable | |

**User's choice:** No ratings for cancelled/disputed (Recommended)
**Notes:** Prevents rating abuse when trade fails

---

## Rating Form

| Option | Description | Selected |
|--------|-------------|----------|
| Stars + optional review | 1-5 stars, optional text (max 500 chars) | ✓ |
| Multi-dimensional ratings | Multiple rating categories | |
| Binary recommend | Thumbs up/down | |

**User's choice:** Stars + optional review (Recommended)
**Notes:** Rating required, text optional

---

## Edit Ratings

| Option | Description | Selected |
|--------|-------------|----------|
| No edits allowed | Rating is final | ✓ |
| 24-hour edit window | Can edit within 24 hours | |
| Edit until other party rates | Edit while other hasn't rated | |

**User's choice:** No edits allowed (Recommended)
**Notes:** Keeps trust signals stable

---

## Rating Display

| Option | Description | Selected |
|--------|-------------|----------|
| Summary + recent reviews | Average rating, count, last 5 reviews | ✓ |
| Rating summary only | No reviews on profile | |
| Full review history | All reviews visible | |

**User's choice:** Summary + recent reviews (Recommended)
**Notes:** Average (1 decimal), count, last 5 visible. Click for full history.

---

## Atomicity & Financial Integrity

| Option | Description | Selected |
|--------|-------------|----------|
| Single transaction with embedded ledger | All updates in one DB transaction | ✓ |
| Double-entry with separate tables | Standard bookkeeping pattern | |
| Event sourcing | Immutable event log | |

**User's choice:** Single transaction with ledger approach
**Notes:** @Transactional, SELECT FOR UPDATE, version fields, idempotency keys, outbox pattern. Invariants: debits=credits, no negative balance, reserved≤available, no double-settle, no bypass dispute.

**Key principle:** Balances are consequence of ledger operations, not arbitrary mutable numbers.

---

## Claude's Discretion

- Exact timeout values for auto-expire
- Dispute evidence submission window
- Maximum review text length enforcement
- Rating reminder notification timing
- Transaction detail page layout
- Dispute resolution admin UI design
- "Request to Buy" button visibility for own listings

---

*Discussion completed: 2026-03-22*