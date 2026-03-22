---
phase: 05
plan: 00
subsystem: notifications-and-chat
tags:
  - wave-0
  - tests
  - scaffolding
dependency_graph:
  requires: []
  provides:
    - backend Phase 5 notification scaffold hooks
    - frontend Phase 5 notification/chat scaffold tests
  affects:
    - backend notification tests
    - frontend notification and listing detail tests
tech_stack:
  added: []
  patterns:
    - JUnit 5 scaffold slots
    - Vitest smoke tests with test.todo placeholders
key_files:
  created:
    - frontend/src/tests/notification-actions.test.tsx
    - frontend/src/tests/notification-preferences.test.tsx
    - frontend/src/tests/messages-page-routing.test.tsx
    - frontend/src/tests/listing-chat-entry.test.tsx
    - .planning/phases/05-notification-detail-actions-quick-notification-settings-and-seller-chat-entry-from-listings/deferred-items.md
  modified:
    - backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java
    - backend/src/test/java/com/tradingplatform/notification/service/NotificationServiceTest.java
    - backend/src/test/java/com/tradingplatform/notification/service/NotificationPushServiceTest.java
decisions:
  - Keep Phase 5 Wave 0 frontend coverage green with smoke assertions plus explicit test.todo reservations for 05-02 and 05-03.
  - Preserve concurrent backend Phase 5 assertions already present in notification tests and add scaffold slots around them instead of reverting them to empty placeholders.
metrics:
  completed_date: 2026-03-22
  duration: 00:10
---

# Phase 05 Plan 00: Wave 0 notification and seller-chat scaffolds Summary

Wave 0 now has explicit backend and frontend test entrypoints for notification actions, quick settings, suppression, normalization, messages deep links, and listing-driven seller chat flow.

## Completed Tasks

### Task 1

- Added explicit Phase 5 scaffold hooks to [NotificationControllerTest.java](/d:/Java/Projects/realTimeTrading/backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java), [NotificationServiceTest.java](/d:/Java/Projects/realTimeTrading/backend/src/test/java/com/tradingplatform/notification/service/NotificationServiceTest.java), and [NotificationPushServiceTest.java](/d:/Java/Projects/realTimeTrading/backend/src/test/java/com/tradingplatform/notification/service/NotificationPushServiceTest.java).
- Commit: `697c4bfa` `test(05-00): add backend phase 5 scaffold slots`
- Verification:
  - `NotificationServiceTest` passed.
  - `NotificationPushServiceTest` passed.
  - `NotificationControllerTest` is currently blocked by a pre-existing application-context failure involving a missing `ChatMapper` bean.

### Task 2

- Created green frontend scaffold tests at [notification-actions.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/notification-actions.test.tsx), [notification-preferences.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/notification-preferences.test.tsx), [messages-page-routing.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/messages-page-routing.test.tsx), and [listing-chat-entry.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/listing-chat-entry.test.tsx).
- Commit: `82875721` `test(05-00): add frontend phase 5 scaffold tests`
- Verification:
  - `npm test -- --run notification-actions.test.tsx notification-preferences.test.tsx messages-page-routing.test.tsx listing-chat-entry.test.tsx` passed.

## Deviations from Plan

### Auto-fixed Issues

1. [Rule 3 - Blocking Issue] Forced Maven verification onto JDK 21
- Found during: Task 1 verification
- Issue: workspace `JAVA_HOME` pointed to `C:\Program Files\Java\jdk1.8.0_131`, causing Surefire to reject Java 21-compiled classes.
- Fix: reran Maven verification with `JAVA_HOME=C:\Program Files\Java\latest\jdk-21`.
- Files modified: none
- Commit: none

### Deferred Issues

1. Pre-existing backend verification blockers
- Found during: Task 1 verification
- Issue: `NotificationControllerTest` fails to start the Spring context because `ChatController` cannot resolve `com.tradingplatform.chat.mapper.ChatMapper`; a clean backend test compile also fails in unrelated listing service tests.
- Action: logged in [deferred-items.md](/d:/Java/Projects/realTimeTrading/.planning/phases/05-notification-detail-actions-quick-notification-settings-and-seller-chat-entry-from-listings/deferred-items.md) and left out of scope for this Wave 0 scaffold plan.

## Known Stubs

- [notification-actions.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/notification-actions.test.tsx#L55) and [notification-actions.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/notification-actions.test.tsx#L56) keep routing assertions as `test.todo` until `05-02` wires notification click navigation.
- [notification-preferences.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/notification-preferences.test.tsx#L33) and [notification-preferences.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/notification-preferences.test.tsx#L34) reserve quick-settings persistence and hydration coverage for `05-02`.
- [messages-page-routing.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/messages-page-routing.test.tsx#L44) and [messages-page-routing.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/messages-page-routing.test.tsx#L45) reserve conversation query-param bootstrap coverage for `05-02`.
- [listing-chat-entry.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/listing-chat-entry.test.tsx#L91) and [listing-chat-entry.test.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/tests/listing-chat-entry.test.tsx#L92) reserve seller chat CTA behavior for `05-03`.
- [NotificationControllerTest.java](/d:/Java/Projects/realTimeTrading/backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java#L186) contains an intentional Wave 0 placeholder hook for stricter controller assertions in `05-01`.

## Verification

- Passed: `npm test -- --run notification-actions.test.tsx notification-preferences.test.tsx messages-page-routing.test.tsx listing-chat-entry.test.tsx`
- Partial/blocked:
  - `mvn "-Dtest=NotificationControllerTest,NotificationServiceTest,NotificationPushServiceTest" test`
  - `NotificationServiceTest` and `NotificationPushServiceTest` passed once JDK 21 was forced.
  - `NotificationControllerTest` remains blocked by the pre-existing `ChatMapper` application-context issue.

## Self-Check

PASSED
