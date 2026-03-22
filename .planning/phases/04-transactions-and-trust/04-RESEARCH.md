# Phase 4: Transactions and Trust - Research

**Researched:** 2026-03-22
**Domain:** Transaction state machine, double-entry ledger, rating system, dispute workflow
**Confidence:** HIGH

## Summary

Phase 4 implements the transaction lifecycle with full transparency through a state machine pattern, financial integrity through double-entry ledger operations, and trust-building through a blind rating system. The architecture follows established project patterns: JPA entities with Lombok, MapStruct for DTOs, Liquibase migrations, Zustand/TanStack Query for frontend state.

**Primary recommendation:** Implement a simple state enum with service-layer validation (not Spring State Machine) for maintainability, use @Transactional with pessimistic locking for financial operations, and implement blind rating with a "pending reveal" table pattern.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Transaction Initiation
- **D-01:** Buyer initiates transaction with "Request to Buy" on listing
- **D-02:** Seller sees pending request and can accept or decline
- **D-03:** Creates explicit handshake before marking sold (no direct seller marking)
- **D-04:** Both parties can initiate from chat ("Request to Buy" button for buyer, "Mark Sold to [User]" for seller in chat - first action wins)

#### Transaction Status Model
- **D-05:** Full lifecycle states: CREATED -> FUNDED -> RESERVED -> DELIVERED -> CONFIRMED -> SETTLED -> COMPLETED
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

#### Cancellation Rules
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

#### Dispute Handling
- **D-15:** Admin-mediated dispute resolution for v1
- **D-16:** Either party can raise dispute after DELIVERED state
- **D-17:** Dispute workflow steps
- **D-18:** Dispute statuses: OPEN, UNDER_REVIEW, WAITING_BUYER_EVIDENCE, WAITING_SELLER_EVIDENCE, RESOLVED_BUYER, RESOLVED_SELLER, PARTIALLY_RESOLVED, ESCALATED, CLOSED

#### Transaction Data Capture
- **D-19 to D-25:** Core transaction data, commercial data, cash flow data, lifecycle timestamps, fulfillment data, communication/trust data, dispute/compliance data

#### Transaction History UI
- **D-26:** Separate "Purchases" and "Sales" tabs in user profile
- **D-27:** Each shows transaction cards with status, item name, price, other party
- **D-28:** Filter by status (active, completed, cancelled)

#### Rating Timing & Visibility
- **D-29:** Rating only available after transaction reaches SETTLED state
- **D-30:** Blind rating system: both parties submit ratings blindly, revealed only after both submit OR after rating window closes
- **D-31:** 14-day rating window after settlement
- **D-32:** Auto-remind at day 7 if not rated
- **D-33:** No ratings allowed for cancelled or disputed transactions that didn't reach settlement
- **D-34:** No edits allowed after submission - rating is final

#### Rating Form
- **D-35:** 1-5 star rating with optional text review (max 500 chars)
- **D-36:** Rating required, text optional
- **D-37:** Buyer rates seller AND seller rates buyer (bidirectional)

#### Rating Display on Profile
- **D-38:** Average rating (1 decimal place) visible on profile
- **D-39:** Total review count visible on profile
- **D-40:** Last 5 reviews visible on profile with click-through to full history
- **D-41:** User entity extended with rating fields: averageRating (BigDecimal), totalRatings (int)

#### Atomicity & Financial Integrity
- **D-42:** Single Transaction record with embedded LedgerEntries for atomicity
- **D-43:** Status change + all balance updates in one DB transaction (rollback all or nothing)
- **D-44:** Implementation pattern:
  - @Transactional on application service method
  - SELECT ... FOR UPDATE or JPA locking for wallet/transaction rows
  - Version fields for optimistic locking where appropriate
  - Unique constraints on idempotency keys
  - Outbox pattern for post-commit events
- **D-45:** Required invariants
- **D-46:** Balances are consequence of ledger operations, not arbitrary mutable numbers

### Claude's Discretion

- Exact timeout values for auto-expire (days until transaction expires without activity)
- Dispute evidence submission window (how many days to submit evidence)
- Maximum review text length enforcement
- Rating reminder notification timing
- Transaction detail page layout and information hierarchy
- Dispute resolution admin UI design
- Whether to show "Request to Buy" button for own listings (disabled or hidden)

### Deferred Ideas (OUT OF SCOPE)

#### v2 Features (Escrow Payment System)
- Integrated payment processing with payment provider APIs
- Escrow holding with automatic fund release
- Offer/counter-offer workflow with expiration
- Per REQUIREMENTS.md TRAN-07 to TRAN-10

#### Future Enhancements
- Automated dispute resolution with evidence scoring
- Partial refunds via dispute resolution
- Transaction value weighting in reputation score (RATE-05, RATE-06)
- Activity indicators (view counts, watching counts) on listings
- Live search updates when new items match saved criteria
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| TRAN-01 | User can mark an item as sold to a specific buyer | Transaction initiation workflow (D-01 to D-04), state machine pattern, Listing status integration |
| TRAN-02 | User can view transaction history (purchases and sales) | Transaction repository queries, pagination, filter by status (D-26 to D-28) |
| TRAN-03 | User can see transaction status (pending, completed, cancelled) | TransactionStatus enum with full lifecycle (D-05, D-06), state transition logic |
| TRAN-04 | Buyer can rate seller after transaction completion | Rating entity, timing rules (D-29), blind rating system (D-30) |
| TRAN-05 | Seller can rate buyer after transaction completion | Bidirectional rating (D-37), same blind rating system |
| TRAN-06 | User can write review text with rating (optional) | Rating entity with reviewText field, max 500 chars (D-35) |
| RATE-01 | User can leave 1-5 star rating after completed transaction | Rating entity with rating field, validation 1-5, SETTLED state requirement (D-29) |
| RATE-02 | User ratings are visible on user profile | User entity extension with averageRating, totalRatings (D-38, D-39, D-41) |
| RATE-03 | User can see average rating score on profile | BigDecimal averageRating on User, aggregation query |
| RATE-04 | User can see total number of ratings received | totalRatings counter on User entity |
</phase_requirements>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4.2 | Backend framework | Project standard, virtual threads support |
| Spring Data JPA | 4.0.x | ORM and data access | Established pattern in project |
| Spring Transaction | Included | @Transactional demarcation | Required for atomic financial operations |
| MapStruct | 1.6.3 | DTO mapping | Established pattern in project |
| Liquibase | Included | Database migrations | Established pattern in project |
| MySQL | 8.x | Primary database | Mandated by project, ACID compliance critical for transactions |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring Kafka | Included | Event publishing | Post-transaction events, notifications |
| Spring Validation | Included | Input validation | Rating validation (1-5), review text length |
| @stomp/stompjs | 7.3.0 | WebSocket client | Real-time transaction updates |
| Zustand | 5.0.12 | Client state | Transaction UI state, filters |
| TanStack Query | 5.x | Server state | Transaction history, ratings API |
| React Router DOM | 7.13.1 | Routing | Transaction detail pages |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Simple state enum | Spring State Machine | Spring State Machine adds complexity for 10 states; service-layer validation sufficient |
| Pessimistic locking | Optimistic locking only | Pessimistic safer for financial operations with concurrent access patterns |
| Blind rating table | Reveal immediately | Blind prevents retaliatory reviews; worth the complexity |

**Installation:**
No new dependencies required. All needed libraries are already in the project.

## Architecture Patterns

### Recommended Project Structure

```
backend/src/main/java/com/tradingplatform/
  transaction/
    entity/
      Transaction.java
      TransactionStatus.java
      LedgerEntry.java
      Dispute.java
      DisputeStatus.java
    dto/
      TransactionRequest.java
      TransactionResponse.java
      TransactionDetailResponse.java
      RatingRequest.java
      RatingResponse.java
      DisputeRequest.java
    repository/
      TransactionRepository.java
      LedgerEntryRepository.java
      RatingRepository.java
      DisputeRepository.java
    service/
      TransactionService.java
      TransactionStateMachine.java
      RatingService.java
      DisputeService.java
    controller/
      TransactionController.java
      RatingController.java
      DisputeController.java
    mapper/
      TransactionMapper.java
      RatingMapper.java

frontend/src/
  pages/
    TransactionsPage.tsx
    TransactionDetailPage.tsx
  components/
    transaction/
      TransactionCard.tsx
      TransactionStatusBadge.tsx
      TransactionTimeline.tsx
      RequestToBuyButton.tsx
    rating/
      RatingForm.tsx
      RatingDisplay.tsx
      ReviewList.tsx
  stores/
    transactionStore.ts
  api/
    transactions.ts
```

### Pattern 1: Simple State Machine with Service-Layer Validation

**What:** Enum-based state with validation methods in service layer instead of Spring State Machine.

**When to use:** When state transitions are well-defined and few in number (~10 states).

**Why not Spring State Machine:** Adds 100+ lines of configuration for what can be done with a switch statement and clear validation methods. More maintainable for this scope.

**Example:**

```java
// Source: Project-established pattern from ListingStatus, adapted for transactions
public enum TransactionStatus {
    CREATED,
    FUNDED,
    RESERVED,
    DELIVERED,
    CONFIRMED,
    SETTLED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    DISPUTED,
    REFUNDED;

    public boolean canTransitionTo(TransactionStatus target) {
        return switch (this) {
            case CREATED -> target == FUNDED || target == CANCELLED || target == EXPIRED;
            case FUNDED -> target == RESERVED || target == CANCELLED || target == DISPUTED;
            case RESERVED -> target == DELIVERED || target == DISPUTED;
            case DELIVERED -> target == CONFIRMED || target == DISPUTED;
            case CONFIRMED -> target == SETTLED || target == DISPUTED;
            case SETTLED -> target == COMPLETED;
            case DISPUTED -> target == RESOLVED_SELLER || target == RESOLVED_BUYER
                || target == PARTIALLY_RESOLVED || target == REFUNDED;
            default -> false;
        };
    }
}
```

### Pattern 2: Double-Entry Ledger with Embedded Entries

**What:** Transaction entity contains a list of LedgerEntry objects, persisted atomically.

**When to use:** Financial operations requiring audit trail and balance integrity.

**Example:**

```java
// Source: Standard double-entry bookkeeping pattern
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Version
    private Long version;  // Optimistic locking

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    // Timestamps for each state
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "funded_at")
    private LocalDateTime fundedAt;
    // ... other state timestamps
}

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;  // BUYER_ESCROW, SELLER_PENDING, PLATFORM_FEE

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;  // DEBIT, CREDIT

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;
}
```

### Pattern 3: Blind Rating with Pending Reveal

**What:** Store ratings hidden until both parties submit or window closes.

**When to use:** Preventing retaliatory negative reviews.

**Example:**

```java
@Entity
@Table(name = "ratings")
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(name = "rater_id", nullable = false)
    private Long raterId;

    @Column(name = "rated_user_id", nullable = false)
    private Long ratedUserId;

    @Column(nullable = false)
    private Integer rating;  // 1-5

    @Column(name = "review_text", length = 500)
    private String reviewText;

    @Column(name = "is_visible", nullable = false)
    private Boolean visible = false;  // Hidden until both parties rate or window closes

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

// Reveal logic in RatingService
public void revealIfNeeded(Long transactionId) {
    List<Rating> ratings = ratingRepository.findByTransactionId(transactionId);
    if (ratings.size() == 2) {
        // Both parties have rated - reveal both
        ratings.forEach(r -> r.setVisible(true));
        ratingRepository.saveAll(ratings);
        updateUserRatingAggregates(ratings.get(0).getRatedUserId());
        updateUserRatingAggregates(ratings.get(1).getRatedUserId());
    }
}
```

### Anti-Patterns to Avoid

- **Storing balance as mutable field:** Balances must be computed from immutable ledger entries. Never allow direct balance updates.
- **Rating immediately visible:** Enables retaliatory reviews. Always use blind rating pattern.
- **Time-based auto-progression:** Per D-08, state transitions require explicit action. Never auto-progress states.
- **Spring State Machine for 10 states:** Over-engineering. Use enum with validation methods.
- **Optimistic locking only for financial ops:** Race conditions can cause incorrect balances. Use pessimistic locking for financial state transitions.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Transaction state machine | Custom state machine framework | Simple enum with service validation | 10 states don't justify Spring State Machine complexity |
| Balance tracking | Mutable balance field | Ledger entries sum query | Audit trail, immutability, correctness |
| Idempotency | Custom idempotency check | Unique constraint on idempotency_key | Database guarantees, no race conditions |
| Concurrent updates | Application-level locks | JPA pessimistic locking | Database-level locking is correct |
| Rating aggregation | Custom aggregation | JPQL AVG/COUNT queries | Efficient, one query |

**Key insight:** Financial data requires database-level guarantees (unique constraints, pessimistic locks) not application-level checks.

## Common Pitfalls

### Pitfall 1: Race Condition on Balance Updates

**What goes wrong:** Two concurrent transactions update the same user's balance, one overwrites the other.

**Why it happens:** Using optimistic locking or no locking on balance updates.

**How to avoid:** Use `@Lock(LockModeType.PESSIMISTIC_WRITE)` on repository methods that read-then-write financial data.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Transaction t WHERE t.id = :id")
Optional<Transaction> findByIdForUpdate(Long id);
```

**Warning signs:** Intermittent balance discrepancies, failed tests under load.

### Pitfall 2: Missing Idempotency on State Transitions

**What goes wrong:** Double-clicking "Confirm Delivery" creates duplicate ledger entries.

**Why it happens:** No idempotency key on state transition requests.

**How to avoid:** Generate idempotency key on frontend, store in ledger entries with unique constraint.

```java
@Transactional
public Transaction confirmDelivery(Long transactionId, String idempotencyKey) {
    if (ledgerEntryRepository.existsByIdempotencyKey(idempotencyKey)) {
        return transactionRepository.findById(transactionId).orElseThrow();
    }
    // Proceed with state transition
}
```

**Warning signs:** Duplicate notifications, unexpected balance changes.

### Pitfall 3: Retaliatory Reviews

**What goes wrong:** Seller sees negative review and leaves retaliatory negative review.

**Why it happens:** Rating visible immediately after submission.

**How to avoid:** Implement blind rating per D-30. Store rating with `visible = false`, reveal only after both parties submit or 14-day window closes.

**Warning signs:** Spike in negative reviews after negative reviews, complaints about "review wars".

### Pitfall 4: Orphaned Listings After Transaction

**What goes wrong:** Transaction cancelled but listing remains RESERVED.

**Why it happens:** Forgot to update listing status on transaction cancellation.

**How to avoid:** Always update listing status in same transaction as transaction state change.

```java
@Transactional
public void cancelTransaction(Long transactionId, Long userId) {
    Transaction transaction = transactionRepository.findByIdForUpdate(transactionId);
    // Validate cancellation allowed...
    transaction.setStatus(TransactionStatus.CANCELLED);
    Listing listing = listingRepository.findById(transaction.getListingId());
    listing.setStatus(ListingStatus.AVAILABLE);  // Must be in same transaction
}
```

**Warning signs:** Listings stuck in RESERVED state with no active transaction.

### Pitfall 5: Missing Transaction Boundaries

**What goes wrong:** Partial state update on error - listing marked SOLD but transaction creation failed.

**Why it happens:** Multiple database operations outside single transaction.

**How to avoid:** All related updates (transaction, listing, ledger entries) must be in single @Transactional method.

**Warning signs:** Data inconsistency, orphaned records.

## Code Examples

### Transaction State Transition

```java
// Source: Project-established service pattern
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    @Transactional
    public Transaction createTransaction(Long buyerId, Long listingId, String idempotencyKey) {
        // Check idempotency
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey);
        }

        Listing listing = listingRepository.findByIdAndDeletedFalse(listingId)
            .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new ApiException(ErrorCode.LISTING_NOT_AVAILABLE);
        }

        Transaction transaction = Transaction.builder()
            .listingId(listingId)
            .buyerId(buyerId)
            .sellerId(listing.getSellerId())
            .amount(listing.getPrice())
            .status(TransactionStatus.CREATED)
            .idempotencyKey(idempotencyKey)
            .build();

        // Reserve listing
        listing.setStatus(ListingStatus.RESERVED);

        transaction = transactionRepository.save(transaction);
        listingRepository.save(listing);

        // Notify seller
        notificationService.createNotification(
            listing.getSellerId(),
            NotificationType.TRANSACTION_UPDATE,
            "New Purchase Request",
            "Someone wants to buy your item: " + listing.getTitle(),
            transaction.getId(),
            "TRANSACTION"
        );

        return transaction;
    }

    @Transactional
    public Transaction transitionStatus(Long transactionId, TransactionStatus newStatus, Long actorId) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!transaction.getStatus().canTransitionTo(newStatus)) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Validate actor can perform this transition
        validateActorForTransition(transaction, newStatus, actorId);

        transaction.setStatus(newStatus);
        setTimestampForStatus(transaction, newStatus);

        // Handle side effects
        handleStatusSideEffects(transaction, newStatus);

        return transactionRepository.save(transaction);
    }
}
```

### Rating Submission with Blind Reveal

```java
@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Rating submitRating(Long transactionId, Long raterId, RatingRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        // Validate transaction is in correct state
        if (transaction.getStatus() != TransactionStatus.SETTLED
            && transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new ApiException(ErrorCode.TRANSACTION_NOT_ELIGIBLE_FOR_RATING);
        }

        // Check rating window (14 days per D-31)
        if (transaction.getSettledAt().plusDays(14).isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.RATING_WINDOW_EXPIRED);
        }

        // Determine rated user
        Long ratedUserId = raterId.equals(transaction.getBuyerId())
            ? transaction.getSellerId()
            : transaction.getBuyerId();

        // Check for existing rating
        if (ratingRepository.existsByTransactionIdAndRaterId(transactionId, raterId)) {
            throw new ApiException(ErrorCode.ALREADY_RATED);
        }

        Rating rating = Rating.builder()
            .transactionId(transactionId)
            .raterId(raterId)
            .ratedUserId(ratedUserId)
            .rating(request.getRating())
            .reviewText(request.getReviewText())
            .visible(false)  // Hidden per D-30
            .build();

        rating = ratingRepository.save(rating);

        // Check if both parties have rated - reveal if so
        revealRatingsIfComplete(transactionId);

        return rating;
    }

    private void revealRatingsIfComplete(Long transactionId) {
        List<Rating> ratings = ratingRepository.findByTransactionId(transactionId);
        if (ratings.size() == 2) {
            ratings.forEach(r -> r.setVisible(true));
            ratingRepository.saveAll(ratings);

            // Update aggregate ratings for both users
            updateUserAggregateRating(ratings.get(0).getRatedUserId());
            updateUserAggregateRating(ratings.get(1).getRatedUserId());

            // Notify users their reviews are visible
            ratings.forEach(r -> notificationService.createNotification(
                r.getRaterId(),
                NotificationType.TRANSACTION_UPDATE,
                "Review Published",
                "Your review has been published. View the transaction for details.",
                transactionId,
                "TRANSACTION"
            ));
        }
    }

    private void updateUserAggregateRating(Long userId) {
        Double avgRating = ratingRepository.calculateAverageRatingForUser(userId);
        Integer totalRatings = ratingRepository.countRatingsForUser(userId);

        User user = userRepository.findById(userId).orElseThrow();
        user.setAverageRating(avgRating != null ? BigDecimal.valueOf(avgRating) : null);
        user.setTotalRatings(totalRatings);
        userRepository.save(user);
    }
}
```

### Pessimistic Locking for Financial Operations

```java
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT t FROM Transaction t WHERE t.buyerId = :buyerId ORDER BY t.createdAt DESC")
    Page<Transaction> findByBuyerId(@Param("buyerId") Long buyerId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sellerId = :sellerId ORDER BY t.createdAt DESC")
    Page<Transaction> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Spring State Machine for all state | Simple enum with validation | Industry trend 2020+ | Reduced complexity, better testability |
| Mutable balance fields | Ledger entry aggregation | Financial systems always | Audit trail, correctness guarantees |
| Immediate rating visibility | Blind rating reveal | E-commerce best practice | Reduced retaliatory reviews |
| Application-level locking | Database pessimistic locks | Always correct | No race conditions |

**Deprecated/outdated:**
- Spring State Machine for simple workflows: Over-engineering for <20 states
- Storing balances directly: Always derive from ledger entries

## Open Questions

1. **Auto-expire timeout value (D-12)**
   - What we know: Must be configurable, applied to CREATED/FUNDED states with no activity
   - What's unclear: Exact number of days (suggested: 7 days for CREATED, 14 days for FUNDED)
   - Recommendation: Start with 7/14 days, make configurable via application.yml

2. **Dispute evidence submission window**
   - What we know: Both parties need time to submit evidence
   - What's unclear: Exact number of days (suggested: 7 days)
   - Recommendation: 7 days from dispute opening, configurable

3. **Rating reminder timing (D-32)**
   - What we know: Auto-remind at day 7 if not rated, window closes at day 14
   - What's unclear: Exact timing within day 7
   - Recommendation: Schedule for midday (12:00) to avoid off-hours notifications

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (backend), Vitest (frontend) |
| Config file | backend: none (Spring Boot auto-config), frontend: vitest.config.ts |
| Quick run command | `mvn test -Dtest=TransactionServiceTest -q` |
| Full suite command | `mvn test -q` |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| TRAN-01 | User can mark item sold to specific buyer | unit | `mvn test -Dtest=TransactionServiceTest#testCreateTransaction -q` | Wave 0 |
| TRAN-02 | User can view transaction history | unit | `mvn test -Dtest=TransactionServiceTest#testGetTransactionHistory -q` | Wave 0 |
| TRAN-03 | User can see transaction status | unit | `mvn test -Dtest=TransactionServiceTest#testStatusTransitions -q` | Wave 0 |
| TRAN-04 | Buyer can rate seller after completion | unit | `mvn test -Dtest=RatingServiceTest#testSubmitRating -q` | Wave 0 |
| TRAN-05 | Seller can rate buyer after completion | unit | `mvn test -Dtest=RatingServiceTest#testBidirectionalRating -q` | Wave 0 |
| TRAN-06 | User can write review text with rating | unit | `mvn test -Dtest=RatingServiceTest#testReviewText -q` | Wave 0 |
| RATE-01 | User can leave 1-5 star rating | unit | `mvn test -Dtest=RatingServiceTest#testRatingValidation -q` | Wave 0 |
| RATE-02 | Ratings visible on profile | unit | `mvn test -Dtest=UserServiceTest#testRatingDisplay -q` | Wave 0 |
| RATE-03 | Average rating score on profile | unit | `mvn test -Dtest=RatingServiceTest#testAggregateRating -q` | Wave 0 |
| RATE-04 | Total rating count on profile | unit | `mvn test -Dtest=RatingServiceTest#testRatingCount -q` | Wave 0 |

### Sampling Rate

- **Per task commit:** `mvn test -Dtest=TransactionServiceTest -q`
- **Per wave merge:** `mvn test -q`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `backend/src/test/java/com/tradingplatform/transaction/service/TransactionServiceTest.java` - covers TRAN-01 to TRAN-03
- [ ] `backend/src/test/java/com/tradingplatform/transaction/service/RatingServiceTest.java` - covers TRAN-04 to TRAN-06, RATE-01 to RATE-04
- [ ] `backend/src/test/java/com/tradingplatform/transaction/service/DisputeServiceTest.java` - covers dispute workflow
- [ ] `frontend/src/tests/transactions.test.tsx` - covers transaction UI
- [ ] Liquibase migration `009-create-transactions-tables.xml` - transactions, ledger_entries, ratings, disputes tables
- [ ] ErrorCode additions: TRANSACTION_NOT_FOUND, TRANSACTION_NOT_ELIGIBLE_FOR_RATING, RATING_WINDOW_EXPIRED, ALREADY_RATED, INVALID_STATUS_TRANSITION, LISTING_NOT_AVAILABLE

## Sources

### Primary (HIGH confidence)

- Project codebase patterns: User.java, Listing.java, ChatService.java, NotificationService.java - established patterns for entities, services, repositories
- CONTEXT.md decisions D-01 to D-46 - locked implementation decisions
- REQUIREMENTS.md - TRAN-01 to TRAN-06, RATE-01 to RATE-04 requirements

### Secondary (MEDIUM confidence)

- Spring Data JPA documentation - pessimistic locking patterns
- Industry best practices - blind rating systems, double-entry bookkeeping

### Tertiary (LOW confidence)

- None - all recommendations based on project-established patterns or locked decisions

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in project
- Architecture: HIGH - Patterns established in existing codebase
- Pitfalls: HIGH - Based on known financial system patterns and project context

**Research date:** 2026-03-22
**Valid until:** 30 days - stable patterns, no fast-moving dependencies