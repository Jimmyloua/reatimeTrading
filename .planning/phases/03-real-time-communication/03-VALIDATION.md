---
phase: 03
slug: real-time-communication
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test (backend), Vitest + Testing Library (frontend) |
| **Config file** | `application-test.yml` (backend), `vitest.config.ts` (frontend) |
| **Quick run command** | `mvn test -Dtest=ChatControllerTest` or `npm test -- --grep "chat"` |
| **Full suite command** | `mvn test` (backend), `npm test` (frontend) |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest={affected test class}` or `npm test -- --grep {pattern}`
- **After every plan wave:** Run `mvn test` (full backend suite)
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | CHAT-01 | integration | `mvn test -Dtest=ChatControllerTest#testCreateConversation` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | CHAT-03 | unit | `mvn test -Dtest=MessageRepositoryTest#testFindByConversationId` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | CHAT-02 | integration | `mvn test -Dtest=ChatWebSocketControllerTest` | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 2 | CHAT-04 | integration | `mvn test -Dtest=ChatServiceTest#testMessagePersistedBeforeDelivery` | ❌ W0 | ⬜ pending |
| 03-02-03 | 02 | 2 | CHAT-05 | integration | `mvn test -Dtest=TypingIndicatorTest` | ❌ W0 | ⬜ pending |
| 03-03-01 | 03 | 3 | NOTF-01 | integration | `mvn test -Dtest=NotificationServiceTest#testMessageNotification` | ❌ W0 | ⬜ pending |
| 03-03-02 | 03 | 3 | NOTF-02 | integration | `mvn test -Dtest=NotificationServiceTest#testItemSoldNotification` | ❌ W0 | ⬜ pending |
| 03-03-03 | 03 | 3 | NOTF-03 | unit | `mvn test -Dtest=NotificationRepositoryTest` | ❌ W0 | ⬜ pending |
| 03-03-04 | 03 | 3 | NOTF-04 | unit | `mvn test -Dtest=NotificationServiceTest#testMarkAsRead` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/tradingplatform/chat/controller/ChatControllerTest.java` — stubs for CHAT-01, CHAT-03
- [ ] `backend/src/test/java/com/tradingplatform/chat/controller/ChatWebSocketControllerTest.java` — stubs for CHAT-02, CHAT-05
- [ ] `backend/src/test/java/com/tradingplatform/chat/service/ChatServiceTest.java` — stubs for CHAT-04
- [ ] `backend/src/test/java/com/tradingplatform/chat/repository/MessageRepositoryTest.java` — stubs for CHAT-03
- [ ] `backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java` — stubs for NOTF-03, NOTF-04
- [ ] `backend/src/test/java/com/tradingplatform/notification/service/NotificationServiceTest.java` — stubs for NOTF-01, NOTF-02, NOTF-04
- [ ] `frontend/src/tests/chat.test.tsx` — UI tests for chat components
- [ ] `frontend/src/tests/notifications.test.tsx` — UI tests for notification components
- [ ] Maven dependency additions: spring-boot-starter-websocket, spring-kafka
- [ ] npm dependency additions: @stomp/stompjs, sockjs-client, @types/sockjs-client

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| WebSocket reconnection UX | CHAT-02 | Requires network simulation | Disconnect network, verify auto-reconnect with exponential backoff |
| Typing indicator debouncing | CHAT-05 | Timing-sensitive | Type rapidly, verify indicator appears within 300ms, disappears after 3s idle |
| Online presence accuracy | CHAT-05 | Multi-session | Open two browser windows, verify presence updates within 5s |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending