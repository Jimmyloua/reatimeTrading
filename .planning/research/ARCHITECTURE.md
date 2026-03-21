# Architecture Research

**Domain:** Second-Hand Digital Device Marketplace
**Researched:** 2026-03-21
**Confidence:** MEDIUM (based on established marketplace patterns; web search unavailable for verification)

## Standard Architecture

### System Overview

```
+-----------------------------------------------------------------------------+
|                           PRESENTATION LAYER                                 |
+-----------------------------------------------------------------------------+
|  +------------+  +------------+  +------------+  +------------+             |
|  |   React    |  |   React    |  |   React    |  |   React    |             |
|  |   Web App  |  |  Mobile    |  |   Admin    |  |  WebSocket |             |
|  |            |  |  (Future)  |  |  Dashboard |  |   Client   |             |
|  +-----+------+  +-----+------+  +-----+------+  +-----+------+             |
|        |               |               |               |                      |
+--------+---------------+---------------+---------------+----------------------+
         |               |               |               |
         v               v               v               v
+-----------------------------------------------------------------------------+
|                           API GATEWAY LAYER                                  |
+-----------------------------------------------------------------------------+
|  +-----------------------------------------------------------------------+  |
|  |                    Spring Boot REST API Gateway                        |  |
|  |   - Authentication/Authorization (JWT)                                  |  |
|  |   - Rate Limiting                                                       |  |
|  |   - Request Routing                                                     |  |
|  |   - WebSocket Endpoint                                                  |  |
|  +-----------------------------------------------------------------------+  |
+-----------------------------------+-----------------------------------------+
                                    |
+-----------------------------------+-----------------------------------------+
|                         APPLICATION LAYER                                    |
+-----------------------------------------------------------------------------+
|  +------------+  +------------+  +------------+  +------------+             |
|  |   User     |  |  Listing   |  |Transaction |  | Messaging  |             |
|  |  Service   |  |  Service   |  |  Service   |  |  Service   |             |
|  +-----+------+  +-----+------+  +-----+------+  +-----+------+             |
|        |               |               |               |                      |
|  +-----+------+  +-----+------+  +-----+------+  +-----+------+             |
|  |  Search    |  |Notification|  |  Escrow   |  | Reputation |             |
|  |  Service   |  |  Service   |  |  Service  |  |  Service   |             |
|  +------------+  +------------+  +------------+  +------------+             |
+-----------------------------------------------------------------------------+
                                    |
+-----------------------------------+-----------------------------------------+
|                          EVENT STREAMING LAYER                               |
+-----------------------------------------------------------------------------+
|  +-----------------------------------------------------------------------+  |
|  |                         Apache Kafka                                    |  |
|  |   Topics:                                                               |  |
|  |   - user-events | listing-events | transaction-events                  |  |
|  |   - chat-messages | notifications | escrow-events                       |  |
|  +-----------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------+
                                    |
+-----------------------------------+-----------------------------------------+
|                             DATA LAYER                                       |
+-----------------------------------------------------------------------------+
|  +------------+  +------------+  +------------+  +------------+             |
|  |   MySQL    |  |   Redis    |  |   Redis    |  |  File      |             |
|  |  (Primary) |  |  (Cache)   |  | (Sessions) |  |  Storage   |             |
|  +------------+  +------------+  +------------+  +------------+             |
+-----------------------------------------------------------------------------+
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| **User Service** | User registration, authentication, profile management, preferences | Spring Boot REST API with JWT auth |
| **Listing Service** | Item CRUD, photo management, pricing, category/brand/model taxonomy | Spring Boot REST API with file storage |
| **Search Service** | Full-text search, geo-location queries, filters, saved searches | MySQL full-text + Redis caching or Elasticsearch |
| **Transaction Service** | Offer/accept workflow, transaction state machine, history | Event-sourced with Kafka |
| **Messaging Service** | Real-time chat, message persistence, conversation threads | WebSocket + Kafka + MySQL |
| **Notification Service** | Push notifications, email triggers, in-app alerts | Kafka consumer with notification providers |
| **Escrow Service** | Payment holding, release conditions, refund handling | Integration with payment providers |
| **Reputation Service** | User ratings, reviews, trust scores, verification status | Aggregates from transaction history |

## Recommended Project Structure

```
src/main/java/com/trading/
+-- config/                    # Spring configuration classes
|   +-- SecurityConfig.java
|   +-- WebSocketConfig.java
|   +-- KafkaConfig.java
|   +-- RedisConfig.java
|
+-- domain/                    # Domain entities (JPA)
|   +-- user/
|   |   +-- User.java
|   |   +-- Profile.java
|   |   +-- Reputation.java
|   +-- listing/
|   |   +-- Listing.java
|   |   +-- ListingPhoto.java
|   |   +-- Category.java
|   |   +-- Brand.java
|   +-- transaction/
|   |   +-- Transaction.java
|   |   +-- Offer.java
|   |   +-- Escrow.java
|   +-- messaging/
|   |   +-- Conversation.java
|   |   +-- Message.java
|   +-- notification/
|       +-- Notification.java
|
+-- repository/                # JPA Repositories
|   +-- UserRepository.java
|   +-- ListingRepository.java
|   +-- TransactionRepository.java
|   +-- MessageRepository.java
|
+-- service/                   # Business logic layer
|   +-- user/
|   |   +-- UserService.java
|   |   +-- AuthService.java
|   +-- listing/
|   |   +-- ListingService.java
|   |   +-- SearchService.java
|   +-- transaction/
|   |   +-- TransactionService.java
|   |   +-- EscrowService.java
|   +-- messaging/
|   |   +-- ChatService.java
|   |   +-- MessageService.java
|   +-- notification/
|       +-- NotificationService.java
|
+-- controller/               # REST API endpoints
|   +-- UserController.java
|   +-- ListingController.java
|   +-- TransactionController.java
|   +-- SearchController.java
|
+-- websocket/                 # WebSocket handlers
|   +-- ChatWebSocketHandler.java
|   +-- NotificationWebSocketHandler.java
|
+-- kafka/                     # Kafka producers/consumers
|   +-- producer/
|   |   +-- EventPublisher.java
|   +-- consumer/
|       +-- NotificationConsumer.java
|       +-- TransactionEventConsumer.java
|
+-- dto/                       # Data Transfer Objects
|   +-- request/
|   +-- response/
|
+-- security/                  # Security utilities
|   +-- JwtTokenProvider.java
|   +-- UserPrincipal.java
|
+-- exception/                 # Custom exceptions
|   +-- GlobalExceptionHandler.java
|
+-- util/                      # Utility classes
    +-- GeoUtils.java
    +-- ValidationUtils.java
```

### Structure Rationale

- **domain/**: Each aggregate has its own package; entities follow DDD bounded contexts
- **service/**: Business logic isolated from controllers; each domain has focused services
- **kafka/**: Event-driven architecture support; producers/consumers separated
- **websocket/**: Real-time communication handlers isolated for WebSocket lifecycle management
- **config/**: All Spring configuration centralized for easy maintenance

## Architectural Patterns

### Pattern 1: Event-Driven Architecture

**What:** Use Kafka as the central nervous system for async communication between services. All significant domain events (listing created, offer made, transaction completed) are published to Kafka topics.

**When to use:** Essential for real-time features (chat, notifications) and maintaining data consistency across bounded contexts.

**Trade-offs:**
- Pros: Decoupled services, scalability, audit trail, replay capability
- Cons: Added complexity, eventual consistency, debugging challenges

**Example:**
```java
// Event Publisher - when listing is created
@Service
public class ListingService {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public Listing createListing(ListingRequest request) {
        Listing listing = listingRepository.save(new Listing(request));
        kafkaTemplate.send("listing-events", new ListingCreatedEvent(listing));
        return listing;
    }
}

// Event Consumer - notify interested users
@KafkaListener(topics = "listing-events")
public void handleListingCreated(ListingCreatedEvent event) {
    List<User> interestedUsers = searchService.findMatchingSavedSearches(event.getListing());
    notificationService.notifyUsers(interestedUsers, event);
}
```

### Pattern 2: CQRS for Read-Heavy Operations

**What:** Separate read models from write models. Listings are read much more frequently than written.

**When to use:** Search and browse functionality where read performance is critical.

**Trade-offs:**
- Pros: Optimized read performance, flexible query models
- Cons: Data duplication, synchronization complexity

**Example:**
```java
// Write model (normalized)
@Entity
public class Listing {
    @Id private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private BigDecimal price;
    // ... normalized fields
}

// Read model (denormalized for search)
@RedisHash("listing:search")
public class ListingSearchView {
    @Id private Long listingId;
    private String title;
    private String categoryName;  // denormalized
    private String brandName;      // denormalized
    private String userName;       // denormalized
    private BigDecimal price;
    private String thumbnailUrl;
    private Double latitude;
    private Double longitude;
    // ... optimized for search display
}
```

### Pattern 3: Saga Pattern for Transactions

**What:** Manage distributed transactions across services using event-driven sagas. Each step publishes an event that triggers the next step.

**When to use:** Transaction workflow (offer -> accept -> payment -> ship -> confirm) spans multiple services.

**Trade-offs:**
- Pros: Eventual consistency, compensating actions, no distributed locks
- Cons: Complexity, must handle failures and compensations

**Example:**
```java
// Transaction Saga States
public enum TransactionState {
    OFFERED,           // Buyer made offer
    ACCEPTED,          // Seller accepted
    PAYMENT_PENDING,   // Awaiting payment
    PAYMENT_HELD,      // Escrow holding funds
    SHIPPED,           // Seller shipped
    DELIVERED,         // Buyer received
    COMPLETED,         // Transaction finished
    DISPUTED,          // Problem reported
    CANCELLED,         // Transaction cancelled
    REFUNDED           // Money returned
}

// Saga orchestrator
@Service
public class TransactionSaga {
    @KafkaListener(topics = "transaction-events")
    public void handleTransactionEvent(TransactionEvent event) {
        switch (event.getType()) {
            case OFFER_ACCEPTED -> initiateEscrow(event);
            case PAYMENT_HELD -> notifySellerToShip(event);
            case SHIPMENT_CONFIRMED -> holdForDelivery(event);
            case DELIVERY_CONFIRMED -> releaseEscrow(event);
            // Compensating actions
            case DISPUTE_OPENED -> freezeEscrow(event);
            case CANCELLED -> initiateRefund(event);
        }
    }
}
```

### Pattern 4: WebSocket for Real-Time Communication

**What:** Persistent bidirectional connection between client and server for instant message delivery.

**When to use:** Chat, live notifications, activity indicators (views, competitive interest).

**Trade-offs:**
- Pros: Low latency, push capability, efficient for frequent updates
- Cons: Connection management, scaling complexity, mobile battery impact

**Example:**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins("*");
        registry.addHandler(notificationHandler, "/ws/notifications")
                .setAllowedOrigins("*");
    }
}

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ChatMessage chatMessage = parseMessage(message);
        kafkaTemplate.send("chat-messages", chatMessage);
    }

    public void sendToUser(String userId, ChatMessage message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(toJson(message)));
        }
    }
}
```

## Data Flow

### Request Flow (Synchronous)

```
[User Action]
    |
    v
[React Client] --HTTP/WebSocket--> [Spring Boot API Gateway]
                                         |
    +------------------------------------+------------------------------------+
    |                                    |                                    |
    v                                    v                                    v
[Controller]                      [WebSocket Handler]                  [Kafka Producer]
    |                                    |                                    |
    v                                    v                                    v
[Service Layer]                  [Message Router]                    [Kafka Topic]
    |                                    |
    v                                    v
[Repository] <---> [MySQL]        [User Session] <---> [Redis]
    |
    v
[Response] <-- Transform <-------- [Result]
```

### Event Flow (Asynchronous)

```
[Domain Event]
    |
    v
[Kafka Producer] --> [Kafka Topic]
                         |
    +--------------------+--------------------+
    |                    |                    |
    v                    v                    v
[Notification        [Search Indexer]    [Transaction
 Consumer]                |               Saga Orchestrator]
    |                    |                    |
    v                    v                    v
[WebSocket Push]    [Redis Cache]       [Escrow Service]
    |
    v
[User Real-Time Update]
```

### Key Data Flows

1. **Listing Creation Flow:**
   - User creates listing -> ListingService validates and saves to MySQL
   - Event published to `listing-events` topic
   - SearchService updates search index (Redis/Elasticsearch)
   - NotificationService checks saved searches and notifies matching users

2. **Transaction Flow:**
   - Buyer makes offer -> TransactionService creates transaction in OFFERED state
   - Seller accepts -> Saga transitions to ACCEPTED, triggers escrow
   - Payment held -> State transitions through SHIPPED, DELIVERED, COMPLETED
   - Each state change publishes event, triggers notifications

3. **Real-Time Chat Flow:**
   - Sender posts message -> MessageService saves to MySQL
   - Event published to `chat-messages` topic
   - WebSocket handler delivers to connected recipient(s)
   - If offline, notification service sends push notification

4. **Search/Discovery Flow:**
   - User searches -> SearchService queries denormalized search view
   - Results cached in Redis (keyed by query hash)
   - Geo-location queries use MySQL spatial indexes or Redis GEO commands
   - Live search updates pushed via WebSocket when new listings match

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k users | Monolith sufficient. Single Spring Boot instance, single MySQL, single Redis. Kafka optional. |
| 1k-100k users | Horizontal scaling of stateless services. Read replicas for MySQL. Redis clustering. Kafka partitions by user/listing ID. |
| 100k+ users | Microservices extraction. Dedicated services for chat, search, notifications. Database sharding by region. CDN for static assets. |

### Scaling Priorities

1. **First bottleneck: Database read load**
   - What breaks: MySQL single instance can't handle search volume
   - How to fix: Read replicas, Redis caching for hot queries, consider Elasticsearch for search

2. **Second bottleneck: WebSocket connections**
   - What breaks: Single server WebSocket connection limit (~65k connections)
   - How to fix: WebSocket clustering with Redis pub/sub for message routing across instances

3. **Third bottleneck: Search performance**
   - What breaks: MySQL full-text search slows with millions of listings
   - How to fix: Migrate to Elasticsearch with dedicated search cluster

## Anti-Patterns

### Anti-Pattern 1: Storing Chat Messages in Redis Only

**What people do:** Use Redis as primary storage for chat messages for speed

**Why it's wrong:** Redis is in-memory; messages lost on restart/crash. Cannot query message history efficiently.

**Do this instead:** Use MySQL for persistent message storage, Redis only for active conversation cache and pub/sub for WebSocket distribution.

### Anti-Pattern 2: Synchronous Escrow Operations

**What people do:** Block transaction flow while waiting for payment provider responses

**Why it's wrong:** External payment APIs are slow/unreliable. Users experience hangs, timeouts cascade.

**Do this instead:** Use event-driven escrow. Publish PAYMENT_REQUESTED event, consumer handles provider API, publishes PAYMENT_COMPLETED or PAYMENT_FAILED. Transaction saga continues asynchronously.

### Anti-Pattern 3: Embedding All Data in JWT Token

**What people do:** Store user profile, reputation score, preferences in JWT to avoid database lookups

**Why it's wrong:** JWT cannot be revoked, stale data issues, token size bloated

**Do this instead:** Store only user ID in JWT. Cache user profile in Redis with TTL. Invalidate cache on profile updates.

### Anti-Pattern 4: Direct Database Queries for Notifications

**What people do:** Poll database to check for new notifications or messages

**Why it's wrong:** High database load, delayed notifications, wastes resources

**Do this instead:** Push notifications via WebSocket. Use Kafka events to trigger notifications. Store notification state but push proactively.

### Anti-Pattern 5: Storing Location Data Without Spatial Indexes

**What people do:** Store lat/lng as decimal columns, query with application-level distance calculation

**Why it's wrong:** Full table scan for every geo-search query, O(n) performance

**Do this instead:** Use MySQL spatial indexes (GEOMETRY columns, SPATIAL INDEX) or Redis GEO commands for O(log n) geo-radius queries.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Payment/Escrow | REST API + Webhooks | Async event-driven; webhook for payment status updates |
| File Storage (Photos) | S3-compatible API | Upload direct to S3 or via backend; CDN for delivery |
| Push Notifications | Firebase/APNs APIs | Called by NotificationService consumer |
| Email Service | REST API (SendGrid, etc.) | Transactional emails for verification, alerts |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| User Service <-> Listing Service | REST + Events | Listing creation needs user info; user deletion cascades via event |
| Listing Service <-> Search Service | Events only | Search index updated via listing-events topic |
| Transaction Service <-> Escrow Service | Events only | Saga orchestrates via transaction-events and escrow-events |
| Messaging Service <-> Notification Service | Events only | New messages trigger notification via chat-messages topic |
| All Services <-> WebSocket Handlers | Redis Pub/Sub | WebSocket servers subscribe to user-specific channels |

## Build Order Implications

Based on dependencies between components, recommended implementation order:

1. **Foundation (Phase 1):**
   - User Service (authentication, profiles)
   - Core infrastructure (Kafka, Redis, MySQL setup)
   - API Gateway with JWT auth

2. **Core Marketplace (Phase 2):**
   - Listing Service (CRUD, photos)
   - Search Service (basic search, no geo yet)
   - Category/Brand/Model taxonomy

3. **Real-Time Features (Phase 3):**
   - Messaging Service + WebSocket handlers
   - Notification Service
   - Kafka event routing

4. **Transactions (Phase 4):**
   - Transaction Service + Saga orchestration
   - Escrow Service (stub/mock for v1)
   - Reputation Service

5. **Advanced Features (Phase 5):**
   - Geo-location search
   - Saved searches with live updates
   - Activity indicators
   - Advanced notification rules

## Sources

- Spring Boot documentation patterns (training knowledge)
- Event-driven architecture patterns (training knowledge)
- CQRS and Event Sourcing patterns (training knowledge)
- Marketplace domain modeling (training knowledge)
- WebSocket scaling patterns (training knowledge)

**Note:** Web search was unavailable during this research session. All findings are based on established architectural patterns and should be verified against current documentation and best practices before implementation.

---
*Architecture research for: Second-Hand Digital Device Marketplace*
*Researched: 2026-03-21*