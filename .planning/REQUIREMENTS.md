# Requirements: Real-Time Trading Platform

**Defined:** 2026-03-21
**Core Value:** Safe, transparent peer-to-peer transactions for second-hand digital devices with real-time communication and reputation-based trust.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [x] **AUTH-01**: User can register with email and password
- [x] **AUTH-02**: User can log in with email and password
- [x] **AUTH-03**: User session persists across browser refresh (JWT-based)
- [x] **AUTH-04**: User can log out from any page

### User Profiles

- [x] **PROF-01**: User can create profile with display name
- [x] **PROF-02**: User can upload avatar image
- [x] **PROF-03**: User can view their own profile with listing count and join date
- [x] **PROF-04**: User can view other users' profiles

### Item Listings

- [x] **LIST-01**: User can create item listing with title, description, and price
- [x] **LIST-02**: User can upload multiple photos for a listing with primary image selection
- [x] **LIST-03**: User can select category for item (hierarchical electronics categories)
- [x] **LIST-04**: User can specify item condition (new, like new, good, fair, poor)
- [x] **LIST-05**: User can specify item location (city/region for local pickup)
- [x] **LIST-06**: User can edit their own listings
- [x] **LIST-07**: User can delete their own listings
- [x] **LIST-08**: User can mark items as sold, available, or reserved

### Discovery and Search

- [x] **DISC-01**: User can browse items by category
- [x] **DISC-02**: User can search items by full-text search (title, description)
- [x] **DISC-03**: User can filter items by price range
- [x] **DISC-04**: User can filter items by condition
- [x] **DISC-05**: User can filter items by location/distance
- [x] **DISC-06**: User can view item detail page with all listing information
- [x] **DISC-07**: User can view seller information on item detail page

### Real-Time Communication

- [x] **CHAT-01**: User can initiate chat with seller about an item
- [x] **CHAT-02**: User can send and receive real-time messages
- [x] **CHAT-03**: User can view chat history with other users
- [x] **CHAT-04**: User receives message persistence (messages stored in database)
- [x] **CHAT-05**: User can see when other party is typing (optional presence indicator)

### Notifications

- [x] **NOTF-01**: User receives real-time notification when receiving a message
- [x] **NOTF-02**: User receives notification when item sells
- [x] **NOTF-03**: User can view notification history
- [x] **NOTF-04**: User can mark notifications as read

### Chat Reliability and Responsive Messaging

- [x] **P6-01**: Seller presence survives reconnects and only transitions to offline after the timeout/stale window is exhausted
- [x] **P6-02**: The same seller presence state stays synchronized across every conversation row and the active thread header
- [x] **P6-03**: Realtime message previews and unread counts remain duplicate-safe during reconnects and REST fallback sends
- [x] **P6-04**: The `/messages` route switches between desktop two-pane and mobile single-pane layouts without losing thread context
- [x] **P6-05**: Redis-backed realtime fan-out distributes message and presence events across app nodes while MySQL remains the source of truth for message durability and ordering

### Transactions

- [x] **TRAN-01**: User can mark an item as sold to a specific buyer
- [x] **TRAN-02**: User can view transaction history (purchases and sales)
- [x] **TRAN-03**: User can see transaction status (pending, completed, cancelled)
- [x] **TRAN-04**: Buyer can rate seller after transaction completion
- [x] **TRAN-05**: Seller can rate buyer after transaction completion
- [x] **TRAN-06**: User can write review text with rating (optional)

### Ratings and Reviews

- [x] **RATE-01**: User can leave 1-5 star rating after completed transaction
- [x] **RATE-02**: User ratings are visible on user profile
- [x] **RATE-03**: User can see average rating score on profile
- [x] **RATE-04**: User can see total number of ratings received

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Enhanced Transactions

- **TRAN-07**: Escrow payment system holds funds until buyer confirms receipt
- **TRAN-08**: User can make offer on item (offer/counter-offer system)
- **TRAN-09**: Seller can accept, reject, or counter offers
- **TRAN-10**: Offers have expiration time

### Advanced Discovery

- **DISC-08**: Live search updates when new items match saved criteria
- **DISC-09**: User can save items to watchlist
- **DISC-10**: User receives notification when watched item price drops

### Enhanced Trust

- **RATE-05**: User has reputation score based on transaction history, ratings, account age
- **RATE-06**: Transaction value weighted in reputation calculation

### Activity Features

- **ACTV-01**: User can see how many people viewed an item
- **ACTV-02**: User can see how many people watching an item
- **ACTV-03**: Real-time activity indicators on item detail page

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Auction/bidding system | Fixed-price model is simpler, faster transactions; offer/counter-offer provides negotiation |
| Integrated payment processing | Regulatory compliance, payment licensing complexity; escrow holding without payment integration for v1 |
| Device verification/inspection | Requires physical infrastructure, logistics; reputation + escrow provides trust alternative |
| Buyer/seller protection insurance | Legal complexity, insurance licensing; escrow with dispute resolution covers trust |
| Business seller tier | Single user type keeps marketplace simple; reputation differentiates sellers naturally |
| Shipping integration | Carrier APIs, tracking, liability complexity; users arrange own shipping per PROJECT.md |
| Social features (following, feed) | Distraction from core transaction purpose; focus on transactional trust signals |
| OAuth login | Email/password sufficient for v1; can add later if needed |
| Mobile app | Web-first implementation; mobile-responsive design for v1 |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 | Complete |
| AUTH-02 | Phase 1 | Complete |
| AUTH-03 | Phase 1 | Complete |
| AUTH-04 | Phase 1 | Complete |
| PROF-01 | Phase 1 | Complete |
| PROF-02 | Phase 1 | Complete |
| PROF-03 | Phase 1 | Complete |
| PROF-04 | Phase 1 | Complete |
| LIST-01 | Phase 2 | Complete |
| LIST-02 | Phase 2 | Complete |
| LIST-03 | Phase 2 | Complete |
| LIST-04 | Phase 2 | Complete |
| LIST-05 | Phase 2 | Complete |
| LIST-06 | Phase 2 | Complete |
| LIST-07 | Phase 2 | Complete |
| LIST-08 | Phase 2 | Complete |
| DISC-01 | Phase 2 | Complete |
| DISC-02 | Phase 2 | Complete |
| DISC-03 | Phase 2 | Complete |
| DISC-04 | Phase 2 | Complete |
| DISC-05 | Phase 2 | Complete |
| DISC-06 | Phase 2 | Complete |
| DISC-07 | Phase 2 | Complete |
| CHAT-01 | Phase 3 | Complete |
| CHAT-02 | Phase 3 | Complete |
| CHAT-03 | Phase 3 | Complete |
| CHAT-04 | Phase 3 | Complete |
| CHAT-05 | Phase 3 | Complete |
| CHAT-06 | Phase 5 | Complete |
| CHAT-07 | Phase 5 | Complete |
| NOTF-01 | Phase 3 | Complete |
| NOTF-02 | Phase 3 | Complete |
| NOTF-03 | Phase 3 | Complete |
| NOTF-04 | Phase 3 | Complete |
| NOTF-05 | Phase 5 | Complete |
| NOTF-06 | Phase 5 | Complete |
| NOTF-07 | Phase 5 | Complete |
| P6-01 | Phase 6 | Complete |
| P6-02 | Phase 6 | Complete |
| P6-03 | Phase 6 | Complete |
| P6-04 | Phase 6 | Complete |
| P6-05 | Phase 6 | Complete |
| TRAN-01 | Phase 4 | Complete |
| TRAN-02 | Phase 4 | Complete |
| TRAN-03 | Phase 4 | Complete |
| TRAN-04 | Phase 4 | Complete |
| TRAN-05 | Phase 4 | Complete |
| TRAN-06 | Phase 4 | Complete |
| RATE-01 | Phase 4 | Complete |
| RATE-02 | Phase 4 | Complete |
| RATE-03 | Phase 4 | Complete |
| RATE-04 | Phase 4 | Complete |

**Coverage:**
- v1 requirements: 43 total
- Mapped to phases: 43
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-21*
*Last updated: 2026-03-21 after initial definition*
