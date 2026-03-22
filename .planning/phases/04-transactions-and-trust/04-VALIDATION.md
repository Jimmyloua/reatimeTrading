---
phase: 04
slug: transactions-and-trust
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 04 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (backend), Vitest (frontend) |
| **Config file** | backend: none (Spring Boot auto-config), frontend: vitest.config.ts |
| **Quick run command** | `mvn test -Dtest=TransactionServiceTest -q` |
| **Full suite command** | `mvn test -q` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=TransactionServiceTest -q` or relevant test class
- **After every plan wave:** Run `mvn test -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | TRAN-01 | unit | `mvn test -Dtest=TransactionServiceTest#testCreateTransaction -q` | ❌ W0 | ⬜ pending |
| 04-01-02 | 01 | 1 | TRAN-02 | unit | `mvn test -Dtest=TransactionServiceTest#testGetTransactionHistory -q` | ❌ W0 | ⬜ pending |
| 04-01-03 | 01 | 1 | TRAN-03 | unit | `mvn test -Dtest=TransactionServiceTest#testStatusTransitions -q` | ❌ W0 | ⬜ pending |
| 04-02-01 | 02 | 1 | TRAN-04 | unit | `mvn test -Dtest=RatingServiceTest#testSubmitRating -q` | ❌ W0 | ⬜ pending |
| 04-02-02 | 02 | 1 | TRAN-05 | unit | `mvn test -Dtest=RatingServiceTest#testBidirectionalRating -q` | ❌ W0 | ⬜ pending |
| 04-02-03 | 02 | 1 | TRAN-06 | unit | `mvn test -Dtest=RatingServiceTest#testReviewText -q` | ❌ W0 | ⬜ pending |
| 04-02-04 | 02 | 1 | RATE-01 | unit | `mvn test -Dtest=RatingServiceTest#testRatingValidation -q` | ❌ W0 | ⬜ pending |
| 04-02-05 | 02 | 1 | RATE-02 | unit | `mvn test -Dtest=UserServiceTest#testRatingDisplay -q` | ❌ W0 | ⬜ pending |
| 04-02-06 | 02 | 1 | RATE-03 | unit | `mvn test -Dtest=RatingServiceTest#testAggregateRating -q` | ❌ W0 | ⬜ pending |
| 04-02-07 | 02 | 1 | RATE-04 | unit | `mvn test -Dtest=RatingServiceTest#testRatingCount -q` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/tradingplatform/transaction/service/TransactionServiceTest.java` — stubs for TRAN-01 to TRAN-03
- [ ] `backend/src/test/java/com/tradingplatform/transaction/service/RatingServiceTest.java` — stubs for TRAN-04 to TRAN-06, RATE-01 to RATE-04
- [ ] `backend/src/test/java/com/tradingplatform/transaction/service/DisputeServiceTest.java` — stubs for dispute workflow
- [ ] `frontend/src/tests/transactions.test.tsx` — stubs for transaction UI
- [ ] Liquibase migration `009-create-transactions-tables.xml` — transactions, ledger_entries, ratings, disputes tables
- [ ] ErrorCode additions: TRANSACTION_NOT_FOUND, TRANSACTION_NOT_ELIGIBLE_FOR_RATING, RATING_WINDOW_EXPIRED, ALREADY_RATED, INVALID_STATUS_TRANSITION, LISTING_NOT_AVAILABLE

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Transaction state timeline UI | TRAN-03 | Visual verification of timeline component | Navigate to transaction detail page, verify all states display correctly with timestamps |
| Blind rating reveal behavior | RATE-01 | Cross-user timing verification | Create transaction, have both users rate, verify simultaneous reveal |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending