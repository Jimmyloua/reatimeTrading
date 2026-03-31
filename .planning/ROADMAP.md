# Roadmap: Real-Time Trading Platform

**Created:** 2026-03-21
**Core Value:** Safe, transparent peer-to-peer transactions for second-hand digital devices with real-time communication and reputation-based trust.
**Granularity:** Standard
**Total v1 Requirements:** 48

## Phases

- [x] **Phase 1: Foundation and User Management** - Users can create accounts, authenticate, and manage their identity on the platform
- [x] **Phase 2: Core Marketplace (Listings and Discovery)** - Users can list items for sale and discover items to buy through browsing and search
- [x] **Phase 3: Real-Time Communication** - Users can communicate in real-time about items and receive timely notifications
- [x] **Phase 4: Transactions and Trust** - Users can complete transactions and build trust through ratings and reviews
- [x] **Phase 5: Notification detail actions, quick notification settings, and seller chat entry from listings** - Users can open notification context quickly and start seller chats directly from listings
- [x] **Phase 6: Chat presence reliability, multi-conversation seller status sync, responsive message layout, and Redis-backed realtime optimization** - Users can trust realtime seller presence and responsive chat behavior across reconnects and multiple nodes
- [x] **Phase 7: Browse category hover filtering, product collections, notification management, and homepage image modules inspired by Goofish** - Users can enter discovery through server-driven content and synchronized notification management
- [x] **Phase 8: Public discovery access and profile surface integration repair** - Restore anonymous discovery flows and truthful profile data across browse, homepage, and public profile surfaces
- [ ] **Phase 9: Transaction rating loop and sold notification closure** - Close the post-sale trust loop so completed transactions unlock ratings and sold notifications
- [ ] **Phase 10: Milestone validation and audit hygiene** - Reconcile stale verification and validation artifacts so the milestone can be archived cleanly

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

**Plans:** 8/8 plans executed

Plans:
- [x] 04-00-PLAN.md - Wave 0 Test Infrastructure (Wave 0) - Database migration, test stubs, ErrorCode additions
- [x] 04-01-PLAN.md - Transaction Backend (Wave 1) - TransactionStatus enum, Transaction/LedgerEntry entities, service, controller
- [x] 04-02-PLAN.md - Rating Backend (Wave 1) - Rating entity, blind rating service, User entity extension
- [x] 04-03-PLAN.md - Dispute Backend (Wave 2) - Dispute entity, service, controller for conflict resolution
- [x] 04-04-PLAN.md - Frontend Transaction UI (Wave 3) - Transaction pages, components, API client
- [x] 04-05-PLAN.md - Frontend Rating UI (Wave 3) - Star rating, review form, profile rating display
- [x] 04-06-PLAN.md - Verification Checkpoint (Wave 4) - End-to-end verification
- [x] 04-07-PLAN.md - Frontend Integration Gap Closure (Wave 5) - Integrate orphaned components into application flow

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
| 4. Transactions and Trust | 8/8 | Complete | 2026-03-22 |
| 5. Notification detail actions, quick notification settings, and seller chat entry from listings | 4/4 | Complete | 2026-03-24 |
| 6. Chat presence reliability, multi-conversation seller status sync, responsive message layout, and Redis-backed realtime optimization | 4/4 | Complete | 2026-03-25 |
| 7. Browse category hover filtering, product collections, notification management, and homepage image modules inspired by Goofish | 8/8 | Complete | 2026-03-26 |
| 8. Public discovery access and profile surface integration repair | 3/3 | Complete | 2026-03-29 |
| 9. Transaction rating loop and sold notification closure | 3/3 | In Progress | - |
| 10. Milestone validation and audit hygiene | 2/2 | In Progress | - |
| 11. Kafka-backed durable ordered chat delivery with outbox publishing, unified message send flow, and frontend reconciliation optimization | 2/4 | In Progress|  |

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
| Notification Actions and Seller Chat Entry | NOTF-05 to NOTF-07, CHAT-06 to CHAT-07 (5) | Phase 5 |
| Chat Reliability and Responsive Messaging | P6-01 to P6-05 (5) | Phase 6 |
| Discovery Merchandising and Notification Management | P7-01 to P7-05 (5) | Phase 7 |

**Total:** 48 requirements mapped to 7 phases

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
Phase 5 depends on Phase 4 (notification detail actions and seller chat entry build on existing messaging and transaction context).
Phase 6 depends on Phase 5 (realtime reliability improves the established notifications and messaging flows).
Phase 7 depends on Phase 6 (discovery merchandising and synchronized notification management build on the existing browse and realtime surfaces).
Phase 8 depends on Phase 7 (it closes the public discovery and profile integration gaps surfaced after the discovery/content milestone work shipped).
Phase 9 depends on Phase 8 (it closes the post-sale transaction, rating, and sold-notification loops after public browse/profile surfaces are repaired).
Phase 10 depends on Phase 9 (it cleans up the remaining validation and audit metadata once the functional gap phases are complete).
Phase 11 depends on Phase 10 (it upgrades the chat delivery architecture after the current milestone gaps and archival blockers are resolved).

---

## v2 Considerations

The following requirements are deferred to v2:

- **TRAN-07 to TRAN-10:** Escrow payment system with offer/counter-offer workflow
- **DISC-08 to DISC-10:** Live search updates, watchlist, price drop notifications
- **RATE-05 to RATE-06:** Enhanced reputation scoring with transaction weighting
- **ACTV-01 to ACTV-03:** Activity indicators (view counts, watching counts, real-time activity)

These should be re-evaluated after v1 launch based on user feedback and usage patterns.

### Phase 5: Notification detail actions, quick notification settings, and seller chat entry from listings

**Goal:** Users can open the relevant context from notifications, control in-app notification preferences, and start or resume seller conversations directly from listing detail.
**Requirements**: NOTF-05, NOTF-06, NOTF-07, CHAT-06, CHAT-07
**Depends on:** Phase 4
**Plans:** 4 plans

Plans:
- [x] 05-00-PLAN.md - Wave 0 Phase 5 Test Scaffolding (Wave 0) - Add backend/frontend failing tests for notification actions, preferences, and listing chat entry
- [x] 05-01-PLAN.md - Notification Preference Backend (Wave 1) - Add persisted notification settings, suppression logic, and canonical reference normalization
- [x] 05-02-PLAN.md - Notification Actions and Messages Deep Links (Wave 2) - Wire quick settings UI, notification routing, and URL-backed message bootstrap
- [x] 05-03-PLAN.md - Listing Detail Seller Chat Entry (Wave 3) - Add seller chat CTA on listing detail and close with a human verification gate

### Phase 6: Chat presence reliability, multi-conversation seller status sync, responsive message layout, and Redis-backed realtime optimization

**Goal:** Users can rely on seller presence, conversation previews, and message delivery across reconnects, repeated seller threads, multiple backend nodes, and mobile or desktop message layouts.
**Requirements**: P6-01, P6-02, P6-03, P6-04, P6-05
**Depends on:** Phase 5
**Plans:** 4 plans

Plans:
- [x] 06-00-PLAN.md - Wave 0 Phase 6 Test Scaffolding (Wave 0) - Add backend and frontend failing tests for Redis presence, shared seller sync, duplicate-safe fallback, and responsive messages layout
- [x] 06-01-PLAN.md - Redis Presence and Cross-Node Fan-Out Backend (Wave 1) - Replace in-memory presence with Redis-backed ephemeral tracking and publish realtime delivery events across nodes
- [x] 06-02-PLAN.md - Shared Seller Presence and Realtime Fallback Frontend (Wave 2) - Normalize seller presence by `otherUserId`, dedupe realtime events, and move refreshes to degraded-mode only
- [x] 06-03-PLAN.md - Responsive Messages Shell and Phase Verification Gate (Wave 3) - Apply the mobile/tablet/desktop shell contract and close with manual verification

### Phase 7: Browse category hover filtering, product collections, notification management, and homepage image modules inspired by Goofish

**Goal:** Users can enter discovery through accessible category disclosures, server-driven homepage modules, curated collections, and a notification center that keeps filters, preferences, and read actions synchronized.
**Requirements**: P7-01, P7-02, P7-03, P7-04, P7-05
**Depends on:** Phase 6
**Plans:** 8 plans

Plans:
- [x] 07-00-PLAN.md - Wave 0 Phase 7 Test Scaffolding (Wave 0) - Add failing frontend/backend tests for browse disclosure, homepage modules, curated content, and filtered notification management
- [x] 07-01-PLAN.md - Backend Content Foundation (Wave 1) - Create content schema, seed data, entities, repositories, and foundation filtering logic for ordered collections and homepage modules
- [x] 07-02-PLAN.md - Backend Notification Management Filters and Visible Read Actions (Wave 1) - Extend notification APIs with URL-backed filters and mark-visible-as-read behavior
- [x] 07-06-PLAN.md - Content API Delivery Contracts (Wave 2) - Add content DTOs, controller endpoints, and frontend content client/type contracts
- [x] 07-03-PLAN.md - Frontend Browse Disclosure and Server-Driven Homepage (Wave 3) - Implement category disclosure, collection-aware browse routing, and server-driven homepage modules using the shared content contracts
- [x] 07-04-PLAN.md - Frontend Notification Management Page Filters (Wave 2) - Add URL-backed notification filters, page-level visible-read actions, and presentational list wiring
- [x] 07-07-PLAN.md - Frontend Notification Dropdown and Preference Sync (Wave 3) - Add dropdown/store synchronization and grouped preference parity
- [x] 07-05-PLAN.md - Phase 7 Verification Gate (Wave 4) - Run full automation and block on human verification for interaction-heavy behavior

### Phase 8: Public discovery access and profile surface integration repair

**Goal:** Anonymous users can enter discovery through homepage, browse, and public profile surfaces without backend authorization mismatches, and profile listing counts reflect actual marketplace activity.
**Requirements**: DISC-01, DISC-02, DISC-03, DISC-04, DISC-05, P7-01, P7-02, P7-03, PROF-03, PROF-04
**Gap Closure:** Closes milestone audit findings for anonymous discovery 401s, public profile mismatches, and hardcoded profile listing counts.
**Depends on:** Phase 7
**Plans:** 3/3 executed

Executed waves:
- [x] 08-01-PLAN.md - Public API authorization alignment (Wave 1) - Open the required browse, content, category, public profile, and public rating endpoints to match the frontend's anonymous surfaces
- [x] 08-02-PLAN.md - Profile listing count and public data integration (Wave 2) - Replace hardcoded profile listing counts with listing-backed aggregates and verify public profile truthfulness
- [x] 08-03-PLAN.md - Anonymous discovery and profile verification gate (Wave 3) - Add regression coverage for homepage, browse, module-entry, and public profile flows

### Phase 9: Transaction rating loop and sold notification closure

**Goal:** Completed transactions reliably unlock the two-sided rating flow, sold listing transitions produce the expected notifications, and profile reputation surfaces reflect real transaction outcomes.
**Requirements**: LIST-08, NOTF-02, TRAN-04, TRAN-05, TRAN-06, RATE-01, RATE-02, RATE-03, RATE-04
**Gap Closure:** Closes milestone audit findings for unreachable rating eligibility, stubbed rating CTA wiring, and orphaned item-sold notifications.
**Depends on:** Phase 8
**Plans:** 3/3 executed, human verification pending

Executed waves:
- [x] 09-01-PLAN.md - Transaction completion and rating eligibility backend (Wave 1) - Repair status transitions so standard completed transactions can be rated and reputation aggregates remain correct
- [x] 09-02-PLAN.md - Rating CTA and sold notification integration (Wave 2) - Wire the frontend review action and emit sold notifications from listing status changes
- [x] 09-03-PLAN.md - Trust loop verification gate (Wave 3) - Add regression coverage for completed transaction, review submission, profile rating visibility, and item-sold notification delivery

### Phase 10: Milestone validation and audit hygiene

**Goal:** The milestone's planning artifacts, validation records, and manual verification status are internally consistent so milestone archival reflects the real shipped state.
**Requirements**: None (process and audit closure)
**Gap Closure:** Closes the stale Phase 06 verification status, Phase 06 validation draft state, and remaining milestone validation debt before archival.
**Depends on:** Phase 9
**Plans:** 2/2 executed, archival blocker review pending

Executed waves:
- [x] 10-01-PLAN.md - Phase 06 verification and validation reconciliation (Wave 1) - Update stale human-verification and Nyquist metadata to reflect the approved shipped state
- [x] 10-02-PLAN.md - Milestone re-audit and archival readiness gate (Wave 2) - Re-run milestone audit, confirm closure, and prepare clean milestone completion

### Phase 11: Kafka-backed durable ordered chat delivery with outbox publishing, unified message send flow, and frontend reconciliation optimization

**Goal:** Chat delivery remains persistence-first under heavy concurrency by moving durable message fan-out to Kafka, unifying REST and WebSocket sends behind one persisted command path, and replacing frontend full-refresh send/reconnect flows with targeted reconciliation.
**Requirements**: CHAT-01, CHAT-02, CHAT-03, CHAT-04, CHAT-05, CHAT-06, CHAT-07, P6-01, P6-02, P6-03, P6-04
**Architecture Note**: Phase 11 supersedes the Phase 6 Redis-backed message fan-out approach for durable chat delivery. Redis remains required for presence and typing, while Kafka becomes the durable ordered message-delivery transport.
**Depends on:** Phase 10
**Plans:** 2/4 plans executed

Plans:
- [x] 11-01-PLAN.md - Durable send foundation and persisted acknowledgment contract (Wave 1) - Add transactional outbox persistence, shared backend send service, and truthful `PERSISTED` sender ACK semantics
- [x] 11-02-PLAN.md - Kafka relay and async delivery consumer pipeline (Wave 2) - Publish outbox rows to Kafka keyed by `conversationId` and move recipient push plus delivery-state updates to async consumers
- [ ] 11-03-PLAN.md - Frontend optimistic reconciliation and reconnect catch-up (Wave 3) - Replace ACK-triggered full refreshes with local reconciliation and `afterMessageId` delta recovery
- [ ] 11-04-PLAN.md - Delivery lifecycle verification gate (Wave 4) - Prove `PERSISTED` to `DELIVERED` behavior, reconnect catch-up, and Redis presence/typing regression safety

---

*Roadmap created: 2026-03-21*
*Phase 1 plans added: 2026-03-21*
*Phase 3 plans added: 2026-03-22*
*Phase 4 plans added: 2026-03-22*
*Phase 7 completed: 2026-03-26*
*Gap closure phases 8-10 added: 2026-03-29*
*Phase 11 added: 2026-03-29*
*Phase 11 planned: 2026-03-29*
