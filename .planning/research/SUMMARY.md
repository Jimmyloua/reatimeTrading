# Project Research Summary

**Project:** Second-Hand Electronics Marketplace (realTimeTrading)
**Domain:** C2C peer-to-peer marketplace for digital devices
**Researched:** 2026-03-21
**Confidence:** MEDIUM-HIGH

## Executive Summary

This is a peer-to-peer marketplace for second-hand electronics (phones, laptops, cameras, etc.) with real-time communication features. Successful marketplace platforms prioritize trust mechanisms (escrow, ratings), liquidity (balanced buyer/seller supply), and reliable real-time communication (persistent chat, instant notifications). The recommended approach uses an event-driven architecture with Kafka as the central nervous system, enabling decoupled services that can scale independently while maintaining real-time capabilities through WebSocket connections.

The core risk profile centers on the classic marketplace chicken-and-egg problem (attracting buyers before sellers or vice versa), transaction trust (buyers need protection from misdescribed items), and technical complexity of real-time features at scale. Mitigation requires seeding supply before launch, implementing escrow from day one, and designing the transaction state machine exhaustively upfront. Technical risks around chat persistence, geographic search performance, and WebSocket scaling are well-understood with established solutions documented in the research.

## Key Findings

### Recommended Stack

The mandated stack (Spring Boot 3.5.x + JDK 21, React 19, MySQL 8, Redis 7, Kafka 4) is modern and well-suited for real-time marketplace requirements. Spring Boot's virtual threads (Project Loom) enable high-concurrency handling for WebSocket connections. React 19's improved concurrent rendering pairs well with real-time UI updates. MySQL provides ACID compliance for transactions while Redis handles sessions, caching, and pub/sub for real-time features. Kafka enables durable message persistence and event-driven architecture.

**Core technologies:**
- **Spring Boot 3.5.x + JDK 21:** Backend framework with virtual threads for high-concurrency real-time features
- **React 19:** Frontend with improved concurrent rendering for real-time updates
- **MySQL 8:** Primary database with JSON columns for flexible item attributes and spatial indexes for geo-location
- **Redis 7:** Session storage, cache layer, pub/sub for WebSocket distribution
- **Apache Kafka 4:** Event streaming for chat persistence, notifications, and async processing
- **Spring WebSocket + STOMP:** Real-time bidirectional communication for chat and notifications
- **TanStack Query + Zustand:** Server state management and client state on frontend

### Expected Features

Research identified a clear hierarchy of features for a marketplace. Table stakes features are expected by users and form the minimum viable product. Differentiators provide competitive advantage but require more investment.

**Must have (table stakes):**
- User registration/login and profiles -- basic identity and trust foundation
- Item listing creation with photos -- core marketplace function
- Browse/search items with categories and conditions -- discovery mechanism
- Real-time in-app chat -- modern UX expectation for buyer-seller communication
- Basic notifications -- message alerts, transaction status updates
- Item status management -- available, reserved, sold states
- Basic ratings/reviews -- post-transaction trust mechanism

**Should have (competitive):**
- Escrow payment system -- major trust differentiator, protects both parties
- Offer/counter-offer system -- negotiation capability increases transaction completion
- Geo-location discovery -- local pickup reduces friction
- Activity indicators -- creates urgency, shows competitive interest

**Defer (v2+):**
- Price suggestions (AI-assisted) -- requires market data and ML model
- Device verification/inspection -- requires physical infrastructure
- Advanced search filters (brand, model, specs) -- nice to have after core works
- Seller analytics dashboard -- post-transaction value-add

### Architecture Approach

The recommended architecture follows event-driven principles with Kafka as the message backbone. All significant domain events (listing created, offer made, transaction completed) publish to Kafka topics, enabling decoupled services that react to events asynchronously. This pattern supports real-time features (chat persistence, notifications), eventual consistency for read models (search index updates), and saga orchestration for complex transactions (offer -> accept -> escrow -> ship -> complete).

**Major components:**
1. **User Service** -- Authentication (JWT), profiles, reputation scores
2. **Listing Service** -- Item CRUD, photos, category taxonomy, pricing
3. **Search Service** -- Full-text search, geo-location queries, saved searches
4. **Transaction Service** -- Offer/accept workflow, state machine, escrow coordination
5. **Messaging Service** -- Real-time chat via WebSocket, message persistence via Kafka, conversation threads
6. **Notification Service** -- Push notifications, email triggers, in-app alerts

### Critical Pitfalls

Research identified several pitfalls that have killed marketplace products. The top five require upfront prevention rather than retroactive fixes.

1. **Liquidity Cold Start** -- Marketplace launches with no buyers or no sellers; users leave within days. Prevent by pre-seeding supply (500+ listings before launch), constraining geography initially, and focusing seller acquisition first.

2. **Trust Vacuum (No Buyer Protection)** -- Buyers receive misdescribed items with no recourse; platform gets reputation as "sketchy." Prevent with escrow system that holds funds until buyer confirms receipt, structured condition grading, and minimum photo requirements.

3. **Transaction State Machine Breakdown** -- Transactions get stuck in undefined states; support overwhelmed with edge cases. Prevent with exhaustive state machine design upfront, explicit timeout handling for every state, and built-in dispute workflow.

4. **Real-Time Message Delivery Without Persistence** -- Chat messages lost when users go offline or servers restart. Prevent by writing every message to database before WebSocket delivery, using Kafka for durable message queuing, and implementing message acknowledgment protocol.

5. **Fake Listings and Inventory Spam** -- Platform fills with duplicate listings, placeholders, and fake items. Prevent with listing limits (e.g., 10 free, then fees), photo requirements, duplicate detection, and price sanity checks.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Foundation and User Management
**Rationale:** All marketplace features depend on user identity and authentication infrastructure. Must establish JWT auth, user profiles, and core database schema before any marketplace features.
**Delivers:** User registration, login, profiles, JWT authentication, Redis session management
**Addresses:** User registration/login, user profile (table stakes)
**Avoids:** JWT without expiration (security pitfall), singleton services (scaling pitfall)
**Stack elements:** Spring Security 7, JJWT, Spring Session, Redis, MySQL
**Architecture:** User Service, security utilities, initial schema migrations

### Phase 2: Core Marketplace (Listings and Discovery)
**Rationale:** With users established, listings become the primary marketplace content. Discovery (search, categories) enables buyers to find sellers. This phase delivers the core listing -> search -> view loop.
**Delivers:** Item listing creation with photos, category taxonomy, condition grading, browse/search, item detail view
**Addresses:** Item listing creation, photo upload, browse/search items, item detail view, category navigation, condition indication (table stakes)
**Avoids:** N+1 queries for listings, geographic search without indexing, no image optimization, hardcoded categories
**Stack elements:** Spring Data JPA, MapStruct, MySQL spatial indexes, S3-compatible storage
**Architecture:** Listing Service, Search Service, file storage integration

### Phase 3: Real-Time Communication
**Rationale:** Real-time chat is a competitive differentiator and enables buyer-seller communication for transactions. Requires WebSocket infrastructure and Kafka for message persistence. Depends on user identity (Phase 1) and listing context (Phase 2).
**Delivers:** Real-time chat with WebSocket, message persistence, conversation threads, basic notifications
**Addresses:** Real-time chat, basic notifications (table stakes)
**Avoids:** Real-time message delivery without persistence, chat history lost on restart, no rate limiting on chat
**Stack elements:** Spring WebSocket, STOMP, Kafka, @stomp/stompjs, sockjs-client
**Architecture:** Messaging Service, Notification Service, WebSocket handlers, Kafka producers/consumers

### Phase 4: Transactions and Trust
**Rationale:** With listings and communication established, transactions become possible. Escrow system provides trust foundation. Transaction state machine must be exhaustive before any escrow code is written. Depends on all previous phases.
**Delivers:** Offer/accept workflow, transaction state machine, escrow coordination (stub for MVP), item status management, basic ratings/reviews
**Addresses:** Item status management, basic ratings/reviews (table stakes); escrow, offer/counter-offer (competitive)
**Avoids:** Transaction state machine breakdown, trust vacuum, review manipulation
**Stack elements:** Kafka sagas, Spring Data JPA, transaction state machine
**Architecture:** Transaction Service, Escrow Service (stub), Reputation Service

### Phase 5: Advanced Features and Polish
**Rationale:** Enhanced discovery and engagement features that differentiate the product. Depends on stable core marketplace and transaction flows.
**Delivers:** Geo-location discovery, activity indicators, saved searches/watchlist, reputation scoring
**Addresses:** Geo-location discovery, activity indicators (competitive); saved items/watchlist (should have)
**Avoids:** Geographic search without proper indexing, performance traps at scale
**Stack elements:** MySQL spatial functions or Redis GEO, Leaflet maps, Kafka event routing
**Architecture:** Enhanced Search Service, reputation scoring, activity tracking

### Phase Ordering Rationale

- **Foundation first:** Authentication and user management are prerequisites for all other features. Cannot have listings without sellers, cannot have transactions without users.
- **Marketplace core before communication:** Listings must exist before users can discuss them. Chat is contextualized around listings.
- **Real-time before transactions:** Buyer-seller communication is essential for negotiation. Transaction disputes often require chat evidence.
- **Transactions before advanced:** Trust mechanisms (escrow, ratings) require completed transactions. Reputation scoring requires transaction history.
- **Advanced last:** Features like geo-location and activity indicators enhance an already-working marketplace but cannot substitute for core functionality.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 4 (Transactions):** Escrow integration is complex; payment provider APIs, legal requirements, and compliance vary by region. May need `/gsd:research-phase` for specific payment integration patterns.
- **Phase 5 (Geo-location):** While spatial indexing is well-documented, the specific UX of location-based discovery and privacy considerations may benefit from additional research.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Foundation):** Spring Security + JWT authentication is well-documented with established patterns.
- **Phase 2 (Listings):** CRUD operations, file upload, search are standard patterns with extensive documentation.
- **Phase 3 (Real-Time):** WebSocket + Kafka for chat has established architectural patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Versions verified directly from GitHub/npm registries. All mandated technologies have mature ecosystems and documented patterns for real-time marketplaces. |
| Features | MEDIUM | Based on training knowledge of marketplace platforms (eBay, Mercari, Swappa, OfferUp). Web search was unavailable for verification; feature landscape should be validated against current competitors. |
| Architecture | MEDIUM | Event-driven patterns, CQRS, and saga patterns are well-established. Specific implementation details for marketplace domain are based on established patterns but should be validated with current Spring Boot documentation. |
| Pitfalls | MEDIUM | Pitfalls identified from established marketplace platform knowledge. Recovery strategies documented. External verification limited by tool availability. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **Escrow payment integration:** Legal/regulatory requirements for payment holding vary by jurisdiction. Recommend consulting payment provider documentation and legal counsel during Phase 4 planning.
- **Geographic coverage strategy:** Research assumed single-region deployment. Multi-region deployment (data residency, latency) not fully addressed; needs research if international expansion planned.
- **Mobile application:** Research focused on web-first implementation. Mobile app (native or PWA) architecture needs separate research if prioritized.
- **Competitive feature verification:** Feature landscape based on training knowledge. Recommend manual verification of current competitor features (eBay, Mercari, Swappa, OfferUp, Facebook Marketplace) before finalizing roadmap.

## Sources

### Primary (HIGH confidence)
- GitHub API -- Spring Boot, React, Spring Security, Spring Data, Spring Kafka, MapStruct, Liquibase, Micrometer, Testcontainers version verification
- npm Registry -- React, TanStack Query, Zustand, React Hook Form, Zod, Axios, STOMP.js, SockJS, React Dropzone, Leaflet, TailwindCSS version verification
- Spring Boot 3.x Documentation -- Virtual threads, Spring Security 7.x, Spring Data 4.x integration patterns

### Secondary (MEDIUM confidence)
- Training knowledge of marketplace platforms -- eBay, Mercari, Swappa, OfferUp, Facebook Marketplace feature patterns
- Established architectural patterns -- Event-driven architecture, CQRS, Saga pattern, WebSocket scaling
- Two-sided marketplace literature -- Y Combinator, a16z marketplace guides for liquidity and trust patterns

### Tertiary (LOW confidence)
- E-commerce security best practices -- OWASP, PCI DSS requirements (recommend verification for Phase 4)
- Real-time systems architecture -- Kafka documentation, WebSocket scalability guides (recommend verification during implementation)

---
*Research completed: 2026-03-21*
*Ready for roadmap: yes*