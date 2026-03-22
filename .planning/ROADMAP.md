# Roadmap: Real-Time Trading Platform

**Created:** 2026-03-21
**Core Value:** Safe, transparent peer-to-peer transactions for second-hand digital devices with real-time communication and reputation-based trust.
**Granularity:** Standard
**Total v1 Requirements:** 42

## Phases

- [x] **Phase 1: Foundation and User Management** - Users can create accounts, authenticate, and manage their identity on the platform
- [x] **Phase 2: Core Marketplace (Listings and Discovery)** - Users can list items for sale and discover items to buy through browsing and search
- [x] **Phase 3: Real-Time Communication** - Users can communicate in real-time about items and receive timely notifications
- [ ] **Phase 4: Transactions and Trust** - Users can complete transactions and build trust through ratings and reviews

## Phase Details

### Phase 1: Foundation and User Management

**Goal:** Users can create accounts, authenticate securely, and manage their identity on the platform.

**Depends on:** Nothing (first phase)

**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, PROF-01, PROF-02, PROF-03, PROF-04

**Success Criteria** (what must be TRUE):
1. New user can register with email/password and receive confirmation
2. Registered user can log in and remain authenticated across browser refreshes (JWT-based session)
3. User can log out from any page and session is terminated
4. User can set display name and upload avatar image visible on their profile
5. User can view any user's profile showing display name, avatar, join date, and listing count

**Plans:** 6/6 plans executed

Plans:
- [x] 01-01-PLAN.md - Backend Project Setup (Wave 1) - User entity, Liquibase, test infrastructure
- [x] 01-02-PLAN.md - JWT Authentication (Wave 2) - Spring Security, auth endpoints
- [x] 01-03-PLAN.md - Profile Backend (Wave 2) - Profile endpoints, avatar upload
- [x] 01-04-PLAN.md - Frontend Auth UI (Wave 3) - React setup, login/register pages
- [x] 01-05-PLAN.md - Profile UI (Wave 4) - Profile page, avatar upload UI
- [x] 01-06-PLAN.md - Verification Checkpoint (Wave 5) - End-to-end verification

**Architecture:** User Service with Spring Security 7, JWT authentication, Spring Session with Redis, MySQL user database

**Stack Elements:** Spring Boot 3.5.x, Spring Security 7, JJWT, Spring Session, Redis 7, MySQL 8

---

### Phase 2: Core Marketplace (Listings and Discovery)

**Goal:** Users can list items for sale with photos and details, and buyers can discover items through browsing, search, and filters.

**Depends on:** Phase 1 (requires user authentication and profiles)

**Requirements:** LIST-01, LIST-02, LIST-03, LIST-04, LIST-05, LIST-06, LIST-07, LIST-08, DISC-01, DISC-02, DISC-03, DISC-04, DISC-05, DISC-06, DISC-07

**Success Criteria** (what must be TRUE):
1. User can create a listing with title, description, price, multiple photos, category, condition, and location
2. User can edit and delete their own listings
3. User can change listing status between available, reserved, and sold
4. User can browse items by category hierarchy (electronics categories)
5. User can search items by full-text search and filter by price range, condition, and location/distance
6. User can view item detail page with all listing information and seller profile link

**Plans:** 6/6 plans executed

Plans:
- [x] 02-00-PLAN.md - Wave 0 Test Stubs - Test infrastructure for Nyquist compliance
- [x] 02-01-PLAN.md - Category Management (Wave 1) - Category entity, repository, hierarchy
- [x] 02-02-PLAN.md - Listing CRUD (Wave 2) - Listing entity, service, image upload
- [x] 02-03-PLAN.md - Listing Discovery (Wave 3) - Search, filters, pagination
- [x] 02-04-PLAN.md - Frontend Listing UI (Wave 4) - Create listing, browse, search pages
- [x] 02-05-PLAN.md - Verification Checkpoint (Wave 5) - End-to-end verification

**Architecture:** Listing Service for CRUD operations, Search Service for discovery, file storage integration for photos, category taxonomy management

**Stack Elements:** Spring Data JPA, MapStruct, MySQL 8 with spatial indexes, S3-compatible storage (or local file storage)

---

### Phase 3: Real-Time Communication

**Goal:** Users can communicate in real-time about items through chat, and receive instant notifications for messages and important events.

**Depends on:** Phase 1 (user identity), Phase 2 (listing context for chats)

**Requirements:** CHAT-01, CHAT-02, CHAT-03, CHAT-04, CHAT-05, NOTF-01, NOTF-02, NOTF-03, NOTF-04

**Success Criteria** (what must be TRUE):
1. User can start a chat conversation with a seller about a specific item
2. User can send and receive messages in real-time with messages persisted to database
3. User can view complete chat history with other users across sessions
4. User receives real-time notification when receiving a new message
5. User receives notification when their item sells, and can view/mark notifications as read

**Plans:** 7/7 plans executed

Plans:
- [x] 03-00-PLAN.md - Wave 0 Test Infrastructure - Test stubs, dependencies
- [x] 03-01-PLAN.md - Chat Entities and REST API (Wave 1) - Conversation, ChatMessage entities, ChatService, ChatController
- [x] 03-02-PLAN.md - Notification Backend (Wave 1) - Notification entity, NotificationService, NotificationController
- [x] 03-03-PLAN.md - WebSocket Real-Time Messaging (Wave 2) - WebSocket config, ChatWebSocketController, typing indicators
- [x] 03-04-PLAN.md - Frontend Chat UI (Wave 3) - MessagesPage, ConversationList, ChatView, WebSocket client
- [x] 03-05-PLAN.md - Frontend Notification UI (Wave 3) - NotificationBell, NotificationDropdown, notification store
- [x] 03-06-PLAN.md - Verification Checkpoint (Wave 4) - End-to-end verification

**Architecture:** Messaging Service with WebSocket handlers, Kafka for message persistence and event streaming, Notification Service for push/email triggers

**Stack Elements:** Spring WebSocket, STOMP, Apache Kafka 4, @stomp/stompjs, sockjs-client

**Critical:** Messages must be written to database before WebSocket delivery to prevent message loss on server restart

---

### Phase 4: Transactions and Trust

**Goal:** Users can complete transactions with clear status tracking, and build trust through ratings and reviews.

**Depends on:** Phase 1 (users), Phase 2 (listings), Phase 3 (communication for negotiation evidence)

**Requirements:** TRAN-01, TRAN-02, TRAN-03, TRAN-04, TRAN-05, TRAN-06, RATE-01, RATE-02, RATE-03, RATE-04

**Success Criteria** (what must be TRUE):
1. User can mark an item as sold to a specific buyer
2. User can view their complete transaction history (purchases and sales) with status
3. Buyer can rate seller (1-5 stars) after transaction completion
4. Seller can rate buyer (1-5 stars) after transaction completion
5. User profile displays average rating score and total number of ratings received

**Plans:** 7 plans

Plans:
- [x] 04-00-PLAN.md - Wave 0 Test Infrastructure (Wave 0) - Database migration, test stubs, ErrorCode additions
- [x] 04-01-PLAN.md - Transaction Backend (Wave 1) - TransactionStatus enum, Transaction/LedgerEntry entities, service, controller
- [x] 04-02-PLAN.md - Rating Backend (Wave 1) - Rating entity, blind rating service, User entity extension
- [x] 04-03-PLAN.md - Dispute Backend (Wave 2) - Dispute entity, service, controller for conflict resolution
- [x] 04-04-PLAN.md - Frontend Transaction UI (Wave 3) - Transaction pages, components, API client
- [x] 04-05-PLAN.md - Frontend Rating UI (Wave 3) - Star rating, review form, profile rating display
- [ ] 04-06-PLAN.md - Verification Checkpoint (Wave 4) - End-to-end verification

**Architecture:** Transaction Service with state machine, Reputation Service for ratings aggregation, transaction history queries

**Stack Elements:** Spring Data JPA, transaction state machine, Kafka events for async processing

**Note:** Escrow payment system (TRAN-07 to TRAN-10) deferred to v2 as per requirements

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation and User Management | 6/6 | Complete | 2026-03-21 |
| 2. Core Marketplace (Listings and Discovery) | 6/6 | Complete | 2026-03-21 |
| 3. Real-Time Communication | 7/7 | Complete | 2026-03-22 |
| 4. Transactions and Trust | 0/7 | Not started | - |

## Coverage

| Category | Requirements | Phase |
|----------|--------------|-------|
| Authentication | AUTH-01 to AUTH-04 (4) | Phase 1 |
| User Profiles | PROF-01 to PROF-04 (4) | Phase 1 |
| Item Listings | LIST-01 to LIST-08 (8) | Phase 2 |
| Discovery and Search | DISC-01 to DISC-07 (7) | Phase 2 |
| Real-Time Communication | CHAT-01 to CHAT-05 (5) | Phase 3 |
| Notifications | NOTF-01 to NOTF-04 (4) | Phase 3 |
| Transactions | TRAN-01 to TRAN-06 (6) | Phase 4 |
| Ratings and Reviews | RATE-01 to RATE-04 (4) | Phase 4 |

**Total:** 42 requirements mapped to 4 phases

## Dependencies

```
Phase 1 (Foundation)
    |
    v
Phase 2 (Listings/Discovery) ----+
    |                             |
    v                             |
Phase 3 (Real-Time Comm) ---------+
    |
    v
Phase 4 (Transactions/Trust)
```

Phase 2 depends on Phase 1 (users must exist to create listings).
Phase 3 depends on Phase 1 (users) and Phase 2 (listings provide chat context).
Phase 4 depends on all previous phases (complete marketplace loop).

---

## v2 Considerations

The following requirements are deferred to v2:

- **TRAN-07 to TRAN-10:** Escrow payment system with offer/counter-offer workflow
- **DISC-08 to DISC-10:** Live search updates, watchlist, price drop notifications
- **RATE-05 to RATE-06:** Enhanced reputation scoring with transaction weighting
- **ACTV-01 to ACTV-03:** Activity indicators (view counts, watching counts, real-time activity)

These should be re-evaluated after v1 launch based on user feedback and usage patterns.

---

*Roadmap created: 2026-03-21*
*Phase 1 plans added: 2026-03-21*
*Phase 3 plans added: 2026-03-22*
*Phase 4 plans added: 2026-03-22*