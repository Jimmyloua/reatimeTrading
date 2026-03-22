---
phase: 05
slug: notification-detail-actions-quick-notification-settings-and-seller-chat-entry-from-listings
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 05 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Frontend: Vitest, Backend: Spring Boot Test / JUnit 5 |
| **Config file** | `frontend/vitest.config.ts` |
| **Quick run command** | `npm test -- --run` and `mvn -Dtest=NotificationControllerTest,NotificationServiceTest,ChatControllerTest,ChatServiceTest test` |
| **Full suite command** | `npm test` and `mvn test` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `npm test -- --run` for touched frontend tests and targeted `mvn -Dtest=... test` for touched backend areas
- **After every plan wave:** Run `npm test` and the targeted backend notification/chat suite
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-00-01 | 00 | 0 | NOTF-06, NOTF-07 | backend scaffold | `mvn -Dtest=NotificationControllerTest,NotificationServiceTest,NotificationPushServiceTest test` | Partial | pending |
| 05-00-02 | 00 | 0 | NOTF-05, NOTF-06, CHAT-06, CHAT-07 | frontend scaffold | `npm test -- --run notification-actions.test.tsx notification-preferences.test.tsx messages-page-routing.test.tsx listing-chat-entry.test.tsx` | No - W0 | pending |
| 05-02-01 | 02 | 1 | NOTF-06 | backend controller/service | `mvn -Dtest=NotificationControllerTest,NotificationServiceTest test` | Partial | pending |
| 05-02-02 | 02 | 1 | NOTF-07 | backend service | `mvn -Dtest=NotificationServiceTest,NotificationPushServiceTest test` | Partial | pending |
| 05-03-01 | 03 | 2 | CHAT-06 | frontend integration | `npm test -- --run listing-chat-entry.test.tsx` | No - W0 | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [ ] `frontend/src/tests/notification-actions.test.tsx` - stubs for notification click-to-route behavior
- [ ] `frontend/src/tests/notification-preferences.test.tsx` - stubs for quick-settings persistence and persisted notification hydration
- [ ] `frontend/src/tests/messages-page-routing.test.tsx` - stubs for query-param conversation bootstrap
- [ ] `frontend/src/tests/listing-chat-entry.test.tsx` - stubs for seller info to conversation flow
- [ ] backend notification preference controller/service tests - coverage for preference reads, updates, and suppression
- [ ] backend normalization tests for notification `referenceType` handling

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Notification dropdown quick settings are understandable and discoverable | NOTF-06 | UX/discoverability judgment | Open bell dropdown, find settings entry, toggle a category, confirm the flow is clear without documentation |
| Clicking seller info opens the intended conversation without confusing self-chat behavior | CHAT-06 | End-to-end navigation confidence | Log in as non-owner, open a listing, click seller chat entry, confirm target conversation opens; repeat as owner and confirm CTA is hidden/blocked |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all missing references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
