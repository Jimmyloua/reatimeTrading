---
phase: 07
slug: browse-category-hover-filtering-product-collections-notification-management-and-homepage-image-modules-inspired-by-goofish
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-25
---

# Phase 07 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest + Testing Library on frontend; JUnit 5 + Spring Boot Test + MockMvc on backend |
| **Config file** | `frontend/vitest.config.ts`; backend uses Spring Boot test defaults |
| **Quick run command** | `cd frontend && npm test -- --run src/tests/browse-category-hover.test.tsx && cd ../backend && mvn -q -Dtest=NotificationControllerTest test` |
| **Full suite command** | `cd frontend && npm test && cd ../backend && mvn test` |
| **Estimated runtime** | ~25 seconds for smoke slices, ~180 seconds for full suite |

---

## Sampling Rate

- **After every task commit:** Run only the smallest touched smoke slice from the verification map below.
- **After every plan wave:** Run the relevant wave smoke slices, not the full frontend and backend suites.
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 07-00-01 | 00 | 0 | P7-01 | frontend integration | `cd frontend && npm test -- browse-category-hover.test.tsx` | Yes | green |
| 07-00-02 | 00 | 0 | P7-02 | frontend integration | `cd frontend && npm test -- homepage-modules.test.tsx browse-category-hover.test.tsx` | Yes | green |
| 07-00-03 | 00 | 0 | P7-03 | backend integration | `cd backend && mvn -Dtest=ContentControllerTest test` | Yes | green |
| 07-00-04 | 00 | 0 | P7-04 | frontend integration | `cd frontend && npm test -- notification-management.test.tsx` | Yes | green |
| 07-00-05 | 00 | 0 | P7-05 | backend integration | `cd backend && mvn -Dtest=NotificationControllerTest test` | Yes | green |
| 07-01-01 | 01 | 1 | P7-03 | backend foundation | `cd backend && mvn -q -Dtest=ContentServiceTest test` | Yes | green |
| 07-02-01 | 02 | 1 | P7-05 | backend service | `cd backend && mvn -q -Dtest=NotificationServiceTest test` | Yes | green |
| 07-02-02 | 02 | 1 | P7-05 | backend controller | `cd backend && mvn -q -Dtest=NotificationControllerTest test` | Yes | green |
| 07-06-01 | 06 | 2 | P7-03 | backend controller | `cd backend && mvn -q -Dtest=ContentControllerTest test` | Yes | green |
| 07-06-02 | 06 | 2 | P7-03 | frontend contract compile | `cd frontend && npx tsc --noEmit` | Yes | green |
| 07-03-01 | 03 | 3 | P7-02, P7-03 | frontend homepage | `cd frontend && npm test -- --run src/tests/homepage-modules.test.tsx` | Yes | green |
| 07-03-02 | 03 | 3 | P7-01, P7-02 | frontend browse | `cd frontend && npm test -- --run src/tests/browse-category-hover.test.tsx` | Yes | green |
| 07-04-01 | 04 | 2 | P7-04, P7-05 | frontend page filtering | `cd frontend && npm test -- --run src/tests/notification-management.test.tsx` | Yes | green |
| 07-07-01 | 07 | 3 | P7-04, P7-05 | frontend dropdown/store sync | `cd frontend && npm test -- --run src/tests/notification-management.test.tsx` | Yes | green |
| 07-07-02 | 07 | 3 | P7-04 | frontend grouped preferences | `cd frontend && npm test -- --run src/tests/notification-preferences.test.tsx src/tests/notification-management.test.tsx` | Yes | green |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [x] `frontend/src/tests/browse-category-hover.test.tsx` - stubs for P7-01 and category preview vs committed filter behavior
- [x] `frontend/src/tests/homepage-modules.test.tsx` - stubs for P7-02 and server-driven homepage module routing
- [x] `frontend/src/tests/notification-management.test.tsx` - stubs for P7-04 tabs, filters, mark-visible-as-read, and unread sync
- [x] `backend/src/test/java/com/tradingplatform/content/controller/ContentControllerTest.java` - stubs for P7-03 homepage module and collection payload ordering
- [x] `backend/src/test/java/com/tradingplatform/content/service/ContentServiceTest.java` - stubs for P7-03 collection membership filtering and fallback behavior

Existing partial coverage reused by later plans:
- [x] `backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java` - pre-existing notification controller coverage extended by Phase 7 backend work
- [x] `frontend/src/tests/notification-preferences.test.tsx` - pre-existing preferences coverage reused and expanded in later frontend notification plans

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Category disclosure behaves correctly across mouse, keyboard, and touch | P7-01 | Interaction quality and accessibility nuances are hard to fully cover with automated tests alone | Verify desktop hover opens preview without changing applied filters, keyboard focus can open and close disclosures, and touch users can expand then commit a category |
| Homepage modules feel coherent on desktop and mobile | P7-02, P7-03 | Visual merchandising, image cropping, and responsive density are easier to assess manually | Open the homepage on mobile and desktop, confirm ordered modules render, images crop safely, and CTA clicks route to expected collection/category/listing targets |
| Notification management keeps unread counts intuitive between bell dropdown and page | P7-04, P7-05 | Cross-surface timing with realtime pushes can still need manual confidence | Trigger new notifications, switch between dropdown and notifications page, apply filters, mark visible notifications read, and confirm unread count remains consistent |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all missing references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-03-26 after targeted automation and manual checkpoint
