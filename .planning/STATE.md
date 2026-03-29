---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: active
last_updated: "2026-03-29T13:30:00.000Z"
progress:
  total_phases: 10
  completed_phases: 7
  total_plans: 43
  completed_plans: 43
---

# State: Real-Time Trading Platform

**Last Updated:** 2026-03-29

## Project Reference

**Core Value:** Safe, transparent peer-to-peer transactions for second-hand digital devices with real-time communication and reputation-based trust.

**Current Focus:** Phase 08 planned - public-discovery-access-and-profile-surface-integration-repair

**Tech Stack:** Spring Boot 3.5.x + JDK 21, React 19, MySQL 8, Redis 7, Kafka 4

## Current Position

Phase: 08
Plan: Planned

### Phase Context

**Phase 8 Goal:** Anonymous users can enter discovery through homepage, browse, and public profile surfaces without backend authorization mismatches, and profile listing counts reflect actual marketplace activity.

**Success Criteria:**

1. Anonymous users can load homepage, browse, and public profile surfaces without hitting backend authorization mismatches
2. Public discovery entry points from categories, modules, and collections resolve successfully into shareable browse URLs
3. Self and public profiles show truthful listing counts derived from listing data
4. Public rating and profile surfaces remain accessible and accurate after the authorization changes

**Requirements:** DISC-01 to DISC-05, P7-01 to P7-03, PROF-03 to PROF-04

## Performance Metrics

| Metric | Value |
|--------|-------|
| Total Phases | 7 |
| Phases Complete | 7 |
| Total Requirements (v1) | 48 |
| Requirements Complete | 48 |
| Current Streak | 26 |
| Phase 01 P01 | 15 minutes | 3 tasks | 9 files |
| Phase 01 P02 | 22min | 3 tasks | 18 files |
| Phase 01 P03 | 35min | 3 tasks | 10 files |
| Phase 01 P04 | 23min | 3 tasks | 29 files |
| Phase 01 P05 | 15 | 3 tasks | 8 files |
| Phase 01 P06 | 25min | 3 tasks | 0 files |
| Phase 02 P00 | 15min | 3 tasks | 20 files |
| Phase 02 P01 | 20 minutes | 3 tasks | 13 files |
| Phase 02 P02 | 30 minutes | 4 tasks | 21 files |
| Phase 02 P03 | 15 minutes | 3 tasks | 7 files |
| Phase 02 P04 | 25 minutes | 5 tasks | 18 files |
| Phase 02 P05 | 15min | 3 tasks | 4 files |
| Phase 03 P00 | 5min | 3 tasks | 10 files |
| Phase 03 P01 | 36min | 3 tasks | 15 files |
| Phase 03 P02 | 19min | 2 tasks | 10 files |
| Phase 03 P03 | 15min | 3 tasks | 14 files |
| Phase 03 P04 | 12 minutes | 3 tasks | 17 files |
| Phase 03 P05 | 10min | 3 tasks | 6 files |
| Phase 03 P06 | 25min | 3 tasks | 4 files |
| Phase 04 P00 | 5min | 4 tasks | 5 files |
| Phase 04 P01 | 18min | 3 tasks | 15 files |
| Phase 04 P02 | 12min | 4 tasks | 10 files |
| Phase 04 P03 | 15min | 2 tasks | 12 files |
| Phase 04 P04 | 10min | 3 tasks | 12 files |
| Phase 04 P05 | 10min | 3 tasks | 9 files |
| Phase 04 P06 | 15min | 3 tasks | 0 files |
| Phase 04 P07 | 5min | 3 tasks | 3 files |
| Phase 05 P00 | 10min | 2 tasks | 9 files |
| Phase 06 P00 | 10min | 2 tasks | 5 files |
| Phase 06 P01 | 17min | 2 tasks | 12 files |
| Phase 06 P02 | 16min | 2 tasks | 11 files |
| Phase 06 P03 | 10min | 2 tasks | 3 files |
| Phase 07 P00 | 15min | 2 tasks | 10 files |
| Phase 07 P01 | 18min | 2 tasks | 10 files |
| Phase 07 P02 | 13min | 2 tasks | 5 files |
| Phase 07 P03 | 20min | 2 tasks | 8 files |
| Phase 07 P04 | 17min | 2 tasks | 5 files |
| Phase 07 P05 | 10min | 2 tasks | 2 files |
| Phase 07 P06 | 12min | 2 tasks | 6 files |
| Phase 07 P07 | 18min | 2 tasks | 8 files |

## Accumulated Context

### Key Decisions

| Decision | Rationale | Made In |
|----------|-----------|---------|
| 4-phase roadmap structure | Derived from natural delivery boundaries: foundation -> marketplace core -> real-time -> transactions | Roadmap creation |
| JWT-based authentication | Standard for SPA architectures, supports stateless scaling | Research |
| Kafka for message persistence | Durable message queuing prevents chat loss, enables real-time features at scale | Research |
| Escrow deferred to v2 | Payment integration complexity; v1 focuses on transaction tracking and reputation | Requirements |
| @Lazy annotation for circular dependencies | Breaks circular dependency between SecurityConfig and UserService | Plan 01-02 |
| Access token 15 min, refresh token 7 days | Security best practice with rotation for enhanced protection | Plan 01-02 |
| 200x200px avatar thumbnail with center-crop | Consistent avatar size, proper aspect ratio handling | Plan 01-03 |
| Local filesystem avatar storage for v1 | Development simplicity, migration to S3 for production | Plan 01-03 |
| Vite 7.x instead of 8.x | @vitejs/plugin-react compatibility, plugin doesn't support Vite 8 yet | Plan 01-04 |
| shadcn base-nova style | Default style for shadcn v4, uses Base UI primitives | Plan 01-04 |
| Vitest for frontend testing | Native Vite integration, jsdom support, fast test execution | Plan 02-00 |
| Adjacency list for category hierarchy | Self-referencing parent_id for categories, simpler than nested sets | Plan 02-01 |
| JPA Specification for dynamic filtering | Composable, type-safe filter predicates for listing search | Plan 02-03 |
| MySQL FULLTEXT for text search | Native full-text search with BOOLEAN MODE for title/description | Plan 02-03 |
| Manual verification checkpoint for UI flows | UI interactions cannot be fully automated; human verification ensures UX quality | Plan 02-05 |
| Polymorphic notification reference (referenceId + referenceType) | Links notifications to conversations, listings, transactions without explicit FKs | Plan 03-02 |
| @Modifying(clearAutomatically = true) for bulk updates | Clears JPA persistence context after UPDATE/DELETE queries to avoid stale data | Plan 03-02 |
| Persistence-first message saving | Messages written to DB before any WebSocket delivery to prevent data loss on server restart | Plan 03-01 |
| WebSocket JWT authentication via ChannelInterceptor | Validates JWT on STOMP CONNECT frame for secure WebSocket connections | Plan 03-03 |
| In-memory presence tracking for v1 | Simple ConcurrentHashMap for development; Redis recommended for production | Plan 03-03 |
| Enum-based state machine for transactions | 10 states don't justify Spring State Machine complexity; enum with canTransitionTo is simpler | Plan 04-01 |
| Pessimistic locking for transaction state transitions | Prevents race conditions on financial operations with concurrent access | Plan 04-01 |
| Idempotency keys on Transaction and LedgerEntry | Database-level duplicate prevention via unique constraints on idempotency_key | Plan 04-01 |
| Tabs component using Base UI primitives | Consistent with existing shadcn v4 pattern using Base UI primitives | Plan 04-04 |
| navigate() for Button-wrapped Links | Match existing codebase pattern; asChild prop not supported in Button component | Plan 04-04 |
| Blind rating system | Ratings hidden until both parties submit to prevent rating bias | Plan 04-02 |
| Component integration via props drilling | Simple prop passing for RequestToBuyButton and ProfileRatingSection avoids state management complexity | Plan 04-07 |
| Redis for ephemeral websocket fan-out only | Keeps durable chat correctness in MySQL while enabling cross-node delivery | Plan 06-01 |
| Redis session TTL plus per-user membership for presence | Prevents one disconnected socket from taking a multi-session seller offline | Plan 06-01 |
| Disposable local redis-server for presence verification | Docker was unavailable, so tests launch Redis directly during the suite | Plan 06-01 |
| Shared seller-level presence store keyed by otherUserId | Keeps repeated seller rows and the active header synchronized from one frontend source of truth | Plan 06-02 |
| Single reconnect rehydrate plus degraded-only polling | Refreshes authoritative thread and list metadata without restoring noisy connected-mode polling | Plan 06-02 |
| Responsive `/messages` stays on one route and switches panes by viewport | Preserves existing `?conversation` deep links while mobile swaps between list and thread panes | Plan 06-03 |
| Bubble readability enforced with 85% mobile and 70% desktop caps | Prevents horizontal overflow while keeping dense conversation layouts readable | Plan 06-03 |
| Browse category disclosure separates preview state from committed filters | Preserves hover and keyboard exploration without mutating the URL until explicit selection | Plan 07-03 |
| Homepage merchandising is server-driven through ordered content records | Keeps homepage modules and curated collections editable via backend content seeds and APIs | Plan 07-01 |
| Notification center state is URL-backed and shared between surfaces | Keeps unread counts, filters, and grouped preferences synchronized between the bell dropdown and full page | Plan 07-04 / 07-07 |

### Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Frontend state management | TanStack Query + Zustand | Server state separation from client state; proven pattern for React 19 |
| Database | MySQL 8 | ACID compliance for transactions, JSON columns for flexible attributes, spatial indexes for geo-location |
| Caching/Sessions | Redis 7 | Fast session storage, pub/sub for WebSocket distribution, caching layer |
| Real-time | Spring WebSocket + STOMP | Bidirectional communication, established Spring ecosystem integration |

### Active Considerations

- **v2 Planning:** Escrow integration (TRAN-07 to TRAN-10), enhanced discovery (DISC-08 to DISC-10), enhanced reputation (RATE-05 to RATE-06)
- **Production Readiness:** Security audit, performance optimization, load testing, S3 migration for file storage

### Roadmap Evolution

- Phase 5 added: Notification detail actions, quick notification settings, and seller chat entry from listings
- Phase 6 added: Chat presence reliability, multi-conversation seller status sync, responsive message layout, and Redis-backed realtime optimization
- Phase 7 added: Browse category hover filtering, product collections, notification management, and homepage image modules inspired by Goofish

## Session Continuity

### Recent Activity

| Date | Action | Result |
|------|--------|--------|
| 2026-03-21 | Project initialized | PROJECT.md created |
| 2026-03-21 | Requirements defined | REQUIREMENTS.md created with 42 v1 requirements |
| 2026-03-21 | Research completed | SUMMARY.md created with phase recommendations |
| 2026-03-21 | Roadmap created | ROADMAP.md created with 4 phases |
| 2026-03-21 | Plan 01-01 completed | Backend project setup with User entity, Liquibase |
| 2026-03-21 | Plan 01-02 completed | JWT authentication with Spring Security |
| 2026-03-21 | Plan 01-03 completed | User profile management with avatar upload |
| 2026-03-21 | Plan 01-04 completed | React frontend with authentication UI |
| 2026-03-21 | Plan 01-05 completed | Profile UI with avatar upload |
| 2026-03-21 | Plan 01-06 completed | Phase 1 verification - all tests pass |
| 2026-03-21 | Phase 1 COMPLETE | 8 requirements verified, phase summary created |
| 2026-03-21 | Plan 02-00 completed | Wave 0 test stubs for Phase 2 |
| 2026-03-21 | Plan 02-01 completed | Category management with hierarchy |
| 2026-03-21 | Plan 02-02 completed | Listing CRUD with images |
| 2026-03-21 | Plan 02-03 completed | Search and discovery with filtering |
| 2026-03-21 | Plan 02-04 completed | Frontend listing UI with forms and pages |
| 2026-03-21 | Plan 02-05 completed | Phase 2 verification - all 115 tests pass |
| 2026-03-21 | Phase 2 COMPLETE | 15 requirements verified, phase summary created |
| 2026-03-22 | Plan 03-00 completed | Wave 0 test stubs for Phase 3 |
| 2026-03-22 | Plan 03-01 completed | Chat entities, repositories, service, controller |
| 2026-03-22 | Plan 03-02 completed | Notification backend with REST API |
| 2026-03-22 | Plan 03-03 completed | WebSocket with STOMP, JWT auth, real-time messaging |
| 2026-03-22 | Plan 03-04 completed | Frontend WebSocket client, chat UI components |
| 2026-03-22 | Plan 03-05 completed | Notification UI with bell icon, dropdown, mark as read |
| 2026-03-22 | Plan 03-06 completed | Phase 3 verification - all 8 test scenarios passed |
| 2026-03-22 | Phase 3 COMPLETE | 9 requirements verified, phase summary created |
| 2026-03-22 | Plan 04-00 completed | Wave 0 test infrastructure for Phase 4 |
| 2026-03-22 | Plan 04-01 completed | Transaction backend with state machine |
| 2026-03-22 | Plan 04-02 completed | Rating backend with blind rating system |
| 2026-03-22 | Plan 04-03 completed | Dispute workflow for conflict resolution |
| 2026-03-22 | Plan 04-04 completed | Frontend transaction UI |
| 2026-03-22 | Plan 04-05 completed | Frontend rating UI |
| 2026-03-22 | Plan 04-06 completed | Phase 4 verification - all manual tests passed |
| 2026-03-22 | Plan 04-07 completed | Frontend integration gap closure |
| 2026-03-22 | Phase 4 COMPLETE | 10 requirements verified, project complete |
| 2026-03-25 | Plan 06-00 completed | Phase 6 backend/frontend red test scaffolding |
| 2026-03-25 | Plan 06-01 completed | Redis fan-out and Redis-backed presence backend shipped |
| 2026-03-25 | Plan 06-02 completed | Shared seller presence sync and duplicate-safe reconnect/fallback frontend shipped |
| 2026-03-25 | Plan 06-03 completed | Responsive `/messages` shell shipped and human verification approved |
| 2026-03-25 | Phase 6 COMPLETE | Seller presence, reconnect fallback, responsive messaging, and Redis-backed fan-out verified |
| 2026-03-26 | Plan 07-00 completed | Phase 7 backend/frontend red test scaffolding shipped |
| 2026-03-26 | Plan 07-01 completed | Backend content schema, seed data, repositories, and service foundation shipped |
| 2026-03-26 | Plan 07-02 completed | Filtered notification retrieval and mark-visible-as-read backend behavior shipped |
| 2026-03-26 | Plan 07-06 completed | Content controller, DTO mapping, and frontend content contracts shipped |
| 2026-03-26 | Plan 07-04 completed | URL-backed notification management page shipped |
| 2026-03-26 | Plan 07-03 completed | Browse disclosure, collection-aware browse routes, and server-driven homepage modules shipped |
| 2026-03-26 | Plan 07-07 completed | Notification dropdown/store synchronization and grouped preferences shipped |
| 2026-03-26 | Plan 07-05 completed | Targeted automation passed and manual verification was approved |
| 2026-03-26 | Phase 7 COMPLETE | Discovery merchandising and notification management synchronization verified |

### Next Actions

1. Plan Phase 08 to close the public discovery and profile integration gaps surfaced by the milestone audit
2. Execute the Phase 08 gap-closure plans, then continue with Phase 09 and Phase 10 before re-auditing the milestone

### Blockers

- Milestone audit reopened 19 requirements across public discovery, profile truthfulness, transaction-to-rating completion, and sold notification delivery

---

*State initialized: 2026-03-21*
*Phase 7 complete: 2026-03-26*
*Gap closure phases 8-10 added: 2026-03-29*
