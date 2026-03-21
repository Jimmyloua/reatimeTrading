# Requirements: Real-Time Trading Platform

**Defined:** 2026-03-21
**Core Value:** Safe, transparent peer-to-peer transactions for second-hand digital devices with real-time communication and reputation-based trust.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [x] **AUTH-01**: User can register with email and password
- [ ] **AUTH-02**: User can log in with email and password
- [ ] **AUTH-03**: User session persists across browser refresh (JWT-based)
- [ ] **AUTH-04**: User can log out from any page

### User Profiles

- [x] **PROF-01**: User can create profile with display name
- [ ] **PROF-02**: User can upload avatar image
- [ ] **PROF-03**: User can view their own profile with listing count and join date
- [ ] **PROF-04**: User can view other users' profiles

### Item Listings

- [ ] **LIST-01**: User can create item listing with title, description, and price
- [ ] **LIST-02**: User can upload multiple photos for a listing with primary image selection
- [ ] **LIST-03**: User can select category for item (hierarchical electronics categories)
- [ ] **LIST-04**: User can specify item condition (new, like new, good, fair, poor)
- [ ] **LIST-05**: User can specify item location (city/region for local pickup)
- [ ] **LIST-06**: User can edit their own listings
- [ ] **LIST-07**: User can delete their own listings
- [ ] **LIST-08**: User can mark items as sold, available, or reserved

### Discovery and Search

- [ ] **DISC-01**: User can browse items by category
- [ ] **DISC-02**: User can search items by full-text search (title, description)
- [ ] **DISC-03**: User can filter items by price range
- [ ] **DISC-04**: User can filter items by condition
- [ ] **DISC-05**: User can filter items by location/distance
- [ ] **DISC-06**: User can view item detail page with all listing information
- [ ] **DISC-07**: User can view seller information on item detail page

### Real-Time Communication

- [ ] **CHAT-01**: User can initiate chat with seller about an item
- [ ] **CHAT-02**: User can send and receive real-time messages
- [ ] **CHAT-03**: User can view chat history with other users
- [ ] **CHAT-04**: User receives message persistence (messages stored in database)
- [ ] **CHAT-05**: User can see when other party is typing (optional presence indicator)

### Notifications

- [ ] **NOTF-01**: User receives real-time notification when receiving a message
- [ ] **NOTF-02**: User receives notification when item sells
- [ ] **NOTF-03**: User can view notification history
- [ ] **NOTF-04**: User can mark notifications as read

### Transactions

- [ ] **TRAN-01**: User can mark an item as sold to a specific buyer
- [ ] **TRAN-02**: User can view transaction history (purchases and sales)
- [ ] **TRAN-03**: User can see transaction status (pending, completed, cancelled)
- [ ] **TRAN-04**: Buyer can rate seller after transaction completion
- [ ] **TRAN-05**: Seller can rate buyer after transaction completion
- [ ] **TRAN-06**: User can write review text with rating (optional)

### Ratings and Reviews

- [ ] **RATE-01**: User can leave 1-5 star rating after completed transaction
- [ ] **RATE-02**: User ratings are visible on user profile
- [ ] **RATE-03**: User can see average rating score on profile
- [ ] **RATE-04**: User can see total number of ratings received

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
| AUTH-02 | Phase 1 | Pending |
| AUTH-03 | Phase 1 | Pending |
| AUTH-04 | Phase 1 | Pending |
| PROF-01 | Phase 1 | Complete |
| PROF-02 | Phase 1 | Pending |
| PROF-03 | Phase 1 | Pending |
| PROF-04 | Phase 1 | Pending |
| LIST-01 | Phase 2 | Pending |
| LIST-02 | Phase 2 | Pending |
| LIST-03 | Phase 2 | Pending |
| LIST-04 | Phase 2 | Pending |
| LIST-05 | Phase 2 | Pending |
| LIST-06 | Phase 2 | Pending |
| LIST-07 | Phase 2 | Pending |
| LIST-08 | Phase 2 | Pending |
| DISC-01 | Phase 2 | Pending |
| DISC-02 | Phase 2 | Pending |
| DISC-03 | Phase 2 | Pending |
| DISC-04 | Phase 2 | Pending |
| DISC-05 | Phase 2 | Pending |
| DISC-06 | Phase 2 | Pending |
| DISC-07 | Phase 2 | Pending |
| CHAT-01 | Phase 3 | Pending |
| CHAT-02 | Phase 3 | Pending |
| CHAT-03 | Phase 3 | Pending |
| CHAT-04 | Phase 3 | Pending |
| CHAT-05 | Phase 3 | Pending |
| NOTF-01 | Phase 3 | Pending |
| NOTF-02 | Phase 3 | Pending |
| NOTF-03 | Phase 3 | Pending |
| NOTF-04 | Phase 3 | Pending |
| TRAN-01 | Phase 4 | Pending |
| TRAN-02 | Phase 4 | Pending |
| TRAN-03 | Phase 4 | Pending |
| TRAN-04 | Phase 4 | Pending |
| TRAN-05 | Phase 4 | Pending |
| TRAN-06 | Phase 4 | Pending |
| RATE-01 | Phase 4 | Pending |
| RATE-02 | Phase 4 | Pending |
| RATE-03 | Phase 4 | Pending |
| RATE-04 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 43 total
- Mapped to phases: 43
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-21*
*Last updated: 2026-03-21 after initial definition*