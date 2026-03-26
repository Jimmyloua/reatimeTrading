# Real-Time Trading Platform

## What This Is

A second-hand digital device marketplace where users can buy and sell electronics with real-time communication and transaction capabilities. The platform supports all categories of digital devices - from consumer electronics (phones, laptops, cameras) to professional equipment (servers, networking gear). Users act as both buyers and sellers in a peer-to-peer marketplace with escrow-secured transactions.

## Core Value

Safe, transparent peer-to-peer transactions for second-hand digital devices with real-time communication and reputation-based trust.

## Requirements

### Validated

- [x] Accessible browse category disclosures keep hover and keyboard preview separate from committed filters - Validated in Phase 7
- [x] Homepage modules and curated collections are delivered from backend content records and route into shareable browse URLs - Validated in Phase 7
- [x] Notification management keeps filters, grouped preferences, and unread counts synchronized across the bell dropdown and `/notifications` - Validated in Phase 7
- [x] Chat presence reliability, seller-status synchronization across conversations, and responsive messages shell - Validated in Phase 6
- [x] Actionable notifications with in-app quick settings and deep links - Validated in Phase 5
- [x] Seller chat entry directly from listing detail - Validated in Phase 5

- [x] User authentication and profile management — Validated in Phase 1
- [x] Item listing with photos, descriptions, and pricing — Validated in Phase 2
- [x] Browse and search items by category, brand, model, price, condition, location — Validated in Phase 2
- [x] Geo-location based item discovery — Validated in Phase 2
- [x] Real-time chat between buyers and sellers — Validated in Phase 3
- [x] Real-time notifications for messages, offers, and transaction updates — Validated in Phase 3

### Active

- [ ] Escrow payment system to secure transactions
- [ ] Reputation system with ratings and transaction history
- [ ] Transaction workflow: offer → acceptance → payment → shipment → confirmation
- [ ] Activity indicators showing item views and competitive interest
- [ ] Live search updates when new items match saved criteria

### Out of Scope

- Device verification/inspection services — requires physical infrastructure
- Integrated payment processing — escrow system will handle payment holding, but actual payment integration deferred
- Buyer/seller protection insurance — legal and financial complexity
- Auction/bidding system — starting with fixed-price listings
- Business seller tier — single user type for v1
- Shipping integration — users arrange their own shipping

## Context

- **Tech Stack**: Spring Boot 3.x with JDK 21 (backend), React (frontend), MySQL (database), Redis (caching/sessions), Kafka (event streaming/messaging)
- **Reference**: Goofish marketplace (https://www.goofish.com) provides UX inspiration for listing and discovery patterns
- **Market**: Second-hand electronics market is growing, with established players like Swappa, Gazelle, and eBay
- **Differentiation**: Real-time communication features combined with escrow security and reputation system
- **Current State**: Phase 7 complete - accessible browse disclosures, server-driven homepage merchandising, curated collections, and synchronized notification management are in place alongside the Phase 6 realtime messaging reliability improvements

## Constraints

- **Tech Stack**: Spring Boot + JDK 21 backend, React frontend, MySQL, Redis, Kafka — mandated by user
- **Architecture**: Must support real-time features (WebSocket/Server-Sent Events for chat, notifications, live updates)
- **Performance**: Low-latency messaging for real-time features
- **Scalability**: Architecture should support horizontal scaling for chat/notification services

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Escrow-based transactions | Builds trust in peer-to-peer transactions, protects both parties | — Pending |
| Dual user roles (buyer/seller) | Simplifies user experience, reduces friction, common pattern in marketplaces | — Pending |
| Real-time messaging with Kafka | Supports high-throughput, scalable real-time features (chat, notifications) | — Pending |
| Geo-location search | Local pickup option is valuable for second-hand items, reduces shipping friction | — Pending |

---

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-26 after Phase 7 completion*
