---
phase: 06
slug: chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-24
---

# Phase 06 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Spring Boot Test + JUnit 5, Vitest 4.1.0 |
| **Config file** | `backend/src/test/resources/application-test.yml`, `frontend/vitest.config.ts` |
| **Quick run command** | `cd backend && mvn -q -Dtest=ChatWebSocketControllerTest,ChatServiceTest test` and `cd frontend && npm test -- --run src/tests/chat-realtime-fallback.test.tsx src/tests/messages-page-routing.test.tsx` |
| **Full suite command** | `cd backend && mvn test` and `cd frontend && npm test` |
| **Estimated runtime** | ~90 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd backend && mvn -q -Dtest=ChatWebSocketControllerTest,ChatServiceTest test` and `cd frontend && npm test -- --run src/tests/chat-realtime-fallback.test.tsx src/tests/messages-page-routing.test.tsx`
- **After every plan wave:** Run `cd backend && mvn test` and `cd frontend && npm test`
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-00-01 | 00 | 0 | P6-01 | backend integration | `cd backend && mvn -q -Dtest=PresenceServiceRedisTest,ChatWebSocketControllerTest test` | no | pending |
| 06-00-02 | 00 | 0 | P6-02 | frontend component/store | `cd frontend && npm test -- --run src/tests/chat-presence-sync.test.tsx` | no | pending |
| 06-00-03 | 00 | 0 | P6-03 | frontend store/component | `cd frontend && npm test -- --run src/tests/chat-realtime-fallback.test.tsx` | no | pending |
| 06-00-04 | 00 | 0 | P6-04 | frontend component | `cd frontend && npm test -- --run src/tests/messages-responsive-layout.test.tsx` | no | pending |
| 06-00-05 | 00 | 0 | P6-05 | backend integration | `cd backend && mvn -q -Dtest=RedisChatFanoutIntegrationTest test` | no | pending |

*Status: `pending` / `green` / `red` / `flaky`*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/tradingplatform/chat/service/PresenceServiceRedisTest.java` - distributed presence TTL, session accounting, stale transition behavior
- [ ] `backend/src/test/java/com/tradingplatform/chat/integration/RedisChatFanoutIntegrationTest.java` - Redis pub/sub to WebSocket fan-out path
- [ ] `frontend/src/tests/chat-presence-sync.test.tsx` - repeated seller rows and active header share one presence source
- [ ] `frontend/src/tests/chat-realtime-fallback.test.tsx` - reconnect and REST fallback reconcile conversation-list previews/unread metadata, degraded polling, duplicate-safe event application
- [ ] `frontend/src/tests/messages-responsive-layout.test.tsx` - mobile single-pane vs desktop two-pane shell

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Seller presence and transport UI remain visually distinct during reconnect transitions | P6-01, P6-02 | Requires checking compact pills, stale copy, and no misleading offline flash across responsive layouts | Open `/messages`, force WebSocket reconnect, verify seller state stays last-known for 30s, transport helper copy appears separately, and repeated seller rows match the active header |
| Mobile messages shell respects single-pane contract and sticky composer behavior | P6-04 | Responsive shell and safe-area behavior are hard to fully assert with current automated coverage | Resize to mobile width, open a conversation, verify back control returns to list, composer stays sticky, and long messages wrap without horizontal scrolling |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
