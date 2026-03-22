# Phase 4: Transactions and Trust - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can complete transactions with clear status tracking and build trust through ratings and reviews. This phase delivers transaction workflow with full lifecycle transparency, buyer/seller rating system, and atomic financial ledger for balance integrity. Escrow payment processing and offer/counter-offer workflow are v2 (deferred per REQUIREMENTS.md).

</domain>

<decisions>
## Implementation Decisions

### Transaction Initiation
- **D-01:** Buyer initiates transaction with "Request to Buy" on listing
- **D-02:** Seller sees pending request and can accept or decline
- **D-03:** Creates explicit handshake before marking sold (no direct seller marking)
- **D-04:** Both parties can initiate from chat ("Request to Buy" button for buyer, "Mark Sold to [User]" for seller in chat - first action wins)

### Transaction Status Model
- **D-05:** Full lifecycle states: CREATED → FUNDED → RESERVED → DELIVERED → CONFIRMED → SETTLED → COMPLETED
- **D-06:** Failure states: CANCELLED, EXPIRED, DISPUTED, REFUNDED
- **D-07:** Users see full transparency of all lifecycle states
- **D-08:** Each state transition requires explicit action from one party (not time-based auto-progression)
- **D-09:** State transition actors:
  - FUNDED: buyer confirms payment sent
  - RESERVED: seller confirms funds received
  - DELIVERED: seller marks shipped/pickup ready
  - CONFIRMED: buyer receives item
  - SETTLED: funds released (automatic)
  - COMPLETED: both parties can rate

### Cancellation Rules
- **D-10:** Cancelable before FUNDED state by either party
- **D-11:** After FUNDED, requires mutual agreement OR dispute resolution
- **D-12:** Auto-expire after X days of inactivity (configurable)
- **D-13:** Cancellation moments allowed:
  - Before buyer pays
  - After order creation but before seller acceptance
  - Before funds are reserved or captured
  - Before seller ships/delivers/transfers
  - When seller misses response deadline
  - When buyer fails to complete payment in time
  - Fraud/risk/compliance check failures
  - Mutual agreement
- **D-14:** NOT cancelable after:
  - Asset delivered/transferred
  - Settlement to seller complete
  - Dispute in final resolution
  - Irreversible external payment cleared (refund path instead)

### Dispute Handling
- **D-15:** Admin-mediated dispute resolution for v1
- **D-16:** Either party can raise dispute after DELIVERED state
- **D-17:** Dispute workflow:
  - User opens dispute with reason and description
  - System marks trade as DISPUTED and freezes funds
  - Both parties get time window to submit evidence
  - Admin/staff reviews case
  - Resolution: full refund, partial refund, release to seller, return-and-refund, or escalation
  - Ledger updates applied atomically
  - Both parties notified, case closed
- **D-18:** Dispute statuses: OPEN, UNDER_REVIEW, WAITING_BUYER_EVIDENCE, WAITING_SELLER_EVIDENCE, RESOLVED_BUYER, RESOLVED_SELLER, PARTIALLY_RESOLVED, ESCALATED, CLOSED

### Transaction Data Capture
- **D-19:** Core transaction data: Transaction ID, Order ID, Buyer ID, Seller ID, Listing/Asset ID, Transaction type, Current status, Creation/update/completion times
- **D-20:** Commercial data: Unit price, Quantity, Total gross amount, Discounts, Fees, Taxes, Net buyer amount, Net seller amount, Currency
- **D-21:** Cash flow data: Payment status, Funding status, Escrow status, Reserved amount, Settled amount, Refunded amount, Payment method, External payment reference, Ledger transaction references
- **D-22:** Lifecycle timestamps: Accepted time, Funded time, Delivered time, Confirmed time, Settled time, Expired time, Cancelled time, Cancellation reason, Auto-cancel deadline, Dispute flag
- **D-23:** Fulfillment data: Delivery method, Delivery proof, Shipping/tracking info, Transfer reference, Completion evidence
- **D-24:** Communication/trust data: Related conversation ID, Offer/counter-offer ID, Snapshot of final agreed terms, User confirmation records, Ratings/review status, Risk score/fraud flags
- **D-25:** Dispute/compliance data: Dispute case ID, Evidence references, Moderator decision, Audit log references, KYC/AML check results, IP/device/session metadata for fraud review

### Transaction History UI
- **D-26:** Separate "Purchases" and "Sales" tabs in user profile
- **D-27:** Each shows transaction cards with status, item name, price, other party
- **D-28:** Filter by status (active, completed, cancelled)

### Rating Timing & Visibility
- **D-29:** Rating only available after transaction reaches SETTLED state
- **D-30:** Blind rating system: both parties submit ratings blindly, revealed only after both submit OR after rating window closes
- **D-31:** 14-day rating window after settlement
- **D-32:** Auto-remind at day 7 if not rated
- **D-33:** No ratings allowed for cancelled or disputed transactions that didn't reach settlement
- **D-34:** No edits allowed after submission - rating is final

### Rating Form
- **D-35:** 1-5 star rating with optional text review (max 500 chars)
- **D-36:** Rating required, text optional
- **D-37:** Buyer rates seller AND seller rates buyer (bidirectional)

### Rating Display on Profile
- **D-38:** Average rating (1 decimal place) visible on profile
- **D-39:** Total review count visible on profile
- **D-40:** Last 5 reviews visible on profile with click-through to full history
- **D-41:** User entity extended with rating fields: averageRating (BigDecimal), totalRatings (int)

### Atomicity & Financial Integrity
- **D-42:** Single Transaction record with embedded LedgerEntries for atomicity
- **D-43:** Status change + all balance updates in one DB transaction (rollback all or nothing)
- **D-44:** Implementation pattern:
  - @Transactional on application service method
  - SELECT ... FOR UPDATE or JPA locking for wallet/transaction rows
  - Version fields for optimistic locking where appropriate
  - Unique constraints on idempotency keys
  - Outbox pattern for post-commit events
- **D-45:** Required invariants:
  - Total debits must equal total credits
  - Balance cannot go negative unless explicitly allowed
  - Reserved funds cannot exceed available funds
  - Settled funds cannot be released twice
  - A completed or canceled transaction cannot be settled again
  - A disputed transaction cannot bypass dispute resolution
- **D-46:** Balances are consequence of ledger operations, not arbitrary mutable numbers

### Claude's Discretion
- Exact timeout values for auto-expire (days until transaction expires without activity)
- Dispute evidence submission window (how many days to submit evidence)
- Maximum review text length enforcement
- Rating reminder notification timing
- Transaction detail page layout and information hierarchy
- Dispute resolution admin UI design
- Whether to show "Request to Buy" button for own listings (disabled or hidden)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — TRAN-01 to TRAN-06, RATE-01 to RATE-04
- `.planning/ROADMAP.md` — Phase 4 details, success criteria, architecture notes
- `.planning/PROJECT.md` — Tech stack mandates (Spring Boot 3.5.x, JDK 21, MySQL 8, Redis 7, Kafka 4)

### Prior Phase Context
- `.planning/phases/01-foundation-and-user-management/01-CONTEXT.md` — User entity, profile requirements (D-06: profile required before interactions)
- `.planning/phases/03-real-time-communication/03-CONTEXT.md` — Notification types including TRANSACTION_UPDATE, ITEM_SOLD, PAYMENT_STATUS

### Architecture Notes
- **ROADMAP.md:** Transaction Service with state machine, Reputation Service for ratings aggregation
- **ROADMAP.md:** Escrow payment system (TRAN-07 to TRAN-10) deferred to v2
- **PROJECT.md Out of Scope:** Integrated payment processing — escrow holding without payment integration for v1

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **User entity** (`backend/src/main/java/com/tradingplatform/user/User.java`) — Extend with rating fields (averageRating, totalRatings)
- **Listing entity** (`backend/src/main/java/com/tradingplatform/listing/entity/Listing.java`) — Transaction references listing, status changes to SOLD on completion
- **ListingStatus enum** (`backend/src/main/java/com/tradingplatform/listing/enums/ListingStatus.java`) — AVAILABLE, RESERVED, SOLD already defined
- **NotificationType enum** (`backend/src/main/java/com/tradingplatform/notification/entity/NotificationType.java`) — TRANSACTION_UPDATE, ITEM_SOLD, PAYMENT_STATUS already defined
- **NotificationService** — Extend for transaction-related notifications
- **Conversation entity** (`backend/src/main/java/com/tradingplatform/chat/entity/Conversation.java`) — Transaction can reference conversation for context
- **Zustand stores** (`frontend/src/stores/`) — Pattern for transaction store
- **TanStack Query** — Pattern for server state management (transaction history, ratings)

### Established Patterns
- **Entity pattern:** JPA with Lombok (@Getter, @Setter, @Builder, auditing listeners)
- **Repository pattern:** Spring Data JPA with derived queries
- **Service layer:** @Service classes with @Transactional methods
- **DTO mapping:** MapStruct for entity-DTO conversion
- **Migrations:** Liquibase for schema changes
- **Frontend state:** Zustand for client state, TanStack Query for server state
- **API pattern:** REST controllers with @RequestMapping, DTOs for request/response

### Integration Points
- **Transaction entity** will need foreign keys to User (buyer, seller), Listing, Conversation (optional)
- **TransactionState entity** for state machine with status enum
- **LedgerEntry entity** for double-entry bookkeeping, references Transaction
- **Rating entity** will need foreign keys to User (rater, rated), Transaction
- **Dispute entity** will need foreign key to Transaction, User (opener)
- **User entity** will need new columns for rating aggregation (average_rating, total_ratings)
- **NotificationService** will send transaction state change notifications
- **Listing status** will change to RESERVED when transaction RESERVED, SOLD when COMPLETED

</code_context>

<specifics>
## Specific Ideas

- Full lifecycle transparency builds trust — users see exactly where their transaction is
- Blind rating prevents retaliatory negative reviews
- Ledger-based balance updates ensure financial integrity — balances derived from immutable ledger entries
- Transaction linked to conversation provides context for dispute resolution
- 14-day rating window balances timely feedback with reasonable flexibility
- Admin-mediated dispute resolution is simplest for v1 — automated resolution can be added later

</specifics>

<deferred>
## Deferred Ideas

### v2 Features (Escrow Payment System)
- Integrated payment processing with payment provider APIs
- Escrow holding with automatic fund release
- Offer/counter-offer workflow with expiration
- Per REQUIREMENTS.md TRAN-07 to TRAN-10

### Future Enhancements
- Automated dispute resolution with evidence scoring
- Partial refunds via dispute resolution
- Transaction value weighting in reputation score (RATE-05, RATE-06)
- Activity indicators (view counts, watching counts) on listings
- Live search updates when new items match saved criteria

</deferred>

---

*Phase: 04-transactions-and-trust*
*Context gathered: 2026-03-22*