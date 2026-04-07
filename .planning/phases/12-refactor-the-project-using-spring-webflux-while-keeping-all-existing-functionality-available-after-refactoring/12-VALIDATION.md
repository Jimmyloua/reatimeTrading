---
phase: 12
slug: refactor-the-project-using-spring-webflux-while-keeping-all-existing-functionality-available-after-refactoring
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-02
---

# Phase 12 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Spring Boot Test + JUnit 5, Vitest |
| **Config file** | `backend/src/test/resources/application-test.yml`, `frontend/vitest.config.ts` |
| **Quick run command** | `cd backend && mvn -q -Dtest=AuthControllerTest,UserControllerTest,AvatarControllerTest test`, `cd backend && mvn -q -Dtest=ListingControllerIT,ContentControllerTest test`, `cd backend && mvn -q -Dtest=ChatControllerTest,ChatWebSocketControllerTest,NotificationControllerTest test`, `cd backend && mvn -q -Dtest=TransactionServiceTest,RatingControllerTest test`, and `cd frontend && npm test -- --run src/tests/create-listing.test.tsx src/tests/chat-realtime-fallback.test.tsx src/tests/notification-actions.test.tsx src/tests/transaction-rating-flow.test.tsx` |
| **Full suite command** | `cd backend && mvn -q -Dtest=AuthControllerTest,UserControllerTest,AvatarControllerTest,ListingControllerIT,ContentControllerTest,ChatControllerTest,ChatWebSocketControllerTest,NotificationControllerTest,TransactionServiceTest,RatingControllerTest test` and `cd frontend && npm test -- --run src/tests/create-listing.test.tsx src/tests/chat-realtime-fallback.test.tsx src/tests/chat-message-ack-reconciliation.test.tsx src/tests/notification-actions.test.tsx src/tests/transaction-rating-flow.test.tsx` |
| **Estimated runtime** | quick checks: 15-70 seconds depending on Spring context startup, full suite: exceeds the current 60-second feedback budget |

---

## Sampling Rate

- **After every task commit:** Run one targeted smoke command for the touched slice
- **After every plan wave:** Run the full suite command
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-00-01 | 00 | 0 | P12-06 | backend contract | `cd backend && mvn -q -Dtest=WebFluxSecurityParityTest,WebFluxListingParityTest test` | no | pending |
| 12-00-02 | 00 | 0 | P12-06 | frontend contract | `cd frontend && npm test -- --run src/tests/chat-webflux-transport-parity.test.tsx src/tests/backend-webflux-contract-parity.test.tsx` | no | pending |
| 12-01-01 | 01 | 1 | P12-01 | backend boot/security | `cd backend && mvn -q -Dtest=WebFluxApplicationBootTest,WebFluxSecurityParityTest test` | no | pending |
| 12-01-02 | 01 | 1 | P12-02 | backend repository/service | `cd backend && mvn -q -Dtest=ReactiveRepositorySmokeTest test` | no | pending |
| 12-02-01 | 02 | 2 | P12-03 | backend controller | `cd backend && mvn -q -Dtest=AuthControllerTest,UserControllerTest,ListingControllerIT,ContentControllerTest test` | yes | passed |
| 12-02-02 | 02 | 2 | P12-05 | frontend integration | `cd frontend && npm test -- --run src/tests/create-listing.test.tsx src/tests/user-profile-page.test.tsx` | yes | passed |
| 12-03-01 | 03 | 2 | P12-04 | backend realtime | `cd backend && mvn -q -Dtest=ChatControllerTest,ChatWebSocketControllerTest,NotificationControllerTest test` | yes | passed |
| 12-03-02 | 03 | 2 | P12-05 | frontend realtime | `cd frontend && npm test -- --run src/tests/chat-realtime-fallback.test.tsx src/tests/chat-message-ack-reconciliation.test.tsx src/tests/notification-actions.test.tsx` | yes | passed |
| 12-04-01 | 04 | 3 | P12-03 | backend transactions | `cd backend && mvn -q -Dtest=TransactionServiceTest,RatingControllerTest,RatingServiceTest test` | yes | passed |
| 12-05-01 | 05 | 3 | P12-01 | backend boot cutover | `cd backend && mvn -q -Dtest=WebFluxApplicationBootTest test` | no | pending |
| 12-06-01 | 06 | 4 | P12-06 | full regression | `cd backend && mvn -q -Dtest=AuthControllerTest,UserControllerTest,AvatarControllerTest,ListingControllerIT,ContentControllerTest,ChatControllerTest,ChatWebSocketControllerTest,NotificationControllerTest,TransactionServiceTest,RatingControllerTest test` and `cd frontend && npm test -- --run src/tests/create-listing.test.tsx src/tests/chat-realtime-fallback.test.tsx src/tests/chat-message-ack-reconciliation.test.tsx src/tests/notification-actions.test.tsx src/tests/transaction-rating-flow.test.tsx` | yes | pending |

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/tradingplatform/migration/WebFluxApplicationBootTest.java` - proves the application boots on the reactive stack
- [ ] `backend/src/test/java/com/tradingplatform/migration/WebFluxSecurityParityTest.java` - locks down public/private route parity during security migration
- [ ] `backend/src/test/java/com/tradingplatform/migration/WebFluxListingParityTest.java` - locks down listing browse/detail/write HTTP contracts
- [ ] `backend/src/test/java/com/tradingplatform/migration/ReactiveRepositorySmokeTest.java` - guards migrated repository and transaction behavior
- [ ] `frontend/src/tests/chat-webflux-transport-parity.test.tsx` - proves reactive realtime transport preserves chat UX expectations
- [ ] `frontend/src/tests/backend-webflux-contract-parity.test.tsx` - proves frontend API adapters still satisfy current backend contracts

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Two-browser realtime chat, typing, presence, and notification behavior still feels identical after the transport migration | P12-04, P12-05 | Browser timing and multi-session socket behavior are hard to prove fully in jsdom | Log in as two users, exchange messages, typing, and presence updates, then confirm notifications and conversation updates still appear without degraded UX |
| Avatar and listing image uploads still save, serve, and render correctly after reactive file handling changes | P12-03, P12-05 | Filesystem upload behavior spans browser, HTTP multipart, and static file serving | Upload an avatar and listing image, refresh, and confirm the saved URLs still render on profile and listing pages |
| Anonymous browse plus authenticated write flows preserve route authorization after reactive security cutover | P12-01, P12-03 | End-to-end browser auth flows cross route matching, token propagation, and redirects | Browse logged out, then log in and create/edit content, confirming public routes remain public and writes remain protected |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [ ] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending - slice regressions are green through Plan 12-04 and the Plan 12-02 upload smoke is green in Surefire, but the phase is still blocked on two honest gaps: the active application remains dual-stack (servlet + reactive) and the current backend integration-test feedback loop already exceeds the 60-second target before the final 12-05 cutover.
