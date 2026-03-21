---
phase: 2
slug: core-marketplace-listings-and-discovery
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-21
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (backend) / Vitest (frontend) |
| **Config file** | `pom.xml` (backend) / `vitest.config.ts` (frontend) |
| **Quick run command** | `mvn test -Dtest=*IT` (backend) / `npm test -- --run` (frontend) |
| **Full suite command** | `mvn verify` (backend) / `npm run test:e2e` (frontend) |
| **Estimated runtime** | ~30 seconds (unit) / ~2 minutes (full) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=*IT` or `npm test -- --run`
- **After every plan wave:** Run `mvn verify` and `npm run test:e2e`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | LIST-03 | unit | `mvn test -Dtest=CategoryRepositoryTest` | ✅ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | LIST-01,02,04,05 | unit | `mvn test -Dtest=ListingRepositoryTest` | ✅ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | LIST-01,06,07,08 | unit | `mvn test -Dtest=ListingServiceTest` | ✅ W0 | ⬜ pending |
| 02-02-02 | 02 | 2 | LIST-02 | unit | `mvn test -Dtest=ListingImageServiceTest` | ✅ W0 | ⬜ pending |
| 02-02-03 | 02 | 2 | LIST-01 to 08 | integration | `mvn test -Dtest=ListingControllerIT` | ✅ W0 | ⬜ pending |
| 02-03-01 | 03 | 3 | DISC-01,02 | unit | `mvn test -Dtest=ListingSearchServiceTest` | ✅ W0 | ⬜ pending |
| 02-03-02 | 03 | 3 | DISC-03,04,05 | unit | `mvn test -Dtest=ListingSpecificationTest` | ✅ W0 | ⬜ pending |
| 02-04-01 | 04 | 4 | LIST-01 to 08 | e2e | `npm test -- create-listing.test.ts` | ✅ W0 | ⬜ pending |
| 02-04-02 | 04 | 4 | DISC-01 to 07 | e2e | `npm test -- browse-listings.test.ts` | ✅ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `src/test/java/.../repository/CategoryRepositoryTest.java` — stubs for LIST-03
- [x] `src/test/java/.../repository/ListingRepositoryTest.java` — stubs for LIST-01,02,04,05
- [x] `src/test/java/.../service/ListingServiceTest.java` — stubs for LIST-01,06,07,08
- [x] `src/test/java/.../service/ListingImageServiceTest.java` — stubs for LIST-02
- [x] `src/test/java/.../controller/ListingControllerIT.java` — integration stubs
- [x] `src/test/java/.../service/ListingSearchServiceTest.java` — stubs for DISC-01,02
- [x] `src/test/java/.../service/ListingSpecificationTest.java` — stubs for DISC-03,04,05
- [x] `frontend/src/tests/create-listing.test.ts` — e2e stubs
- [x] `frontend/src/tests/browse-listings.test.ts` — e2e stubs

*Wave 0 plan created: 02-00-PLAN.md*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Image upload UI flow | LIST-02 | File upload UX best verified visually | Upload multiple images, set primary, verify display order |
| Category selection UX | LIST-03 | Hierarchical dropdown interaction | Navigate 3-level category tree, verify selection |
| Full-text search relevance | DISC-02 | Search quality assessment | Search "laptop gaming", verify ranking and results |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending