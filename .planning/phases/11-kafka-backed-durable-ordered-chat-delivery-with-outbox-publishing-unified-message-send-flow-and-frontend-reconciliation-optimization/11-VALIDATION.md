---
phase: 11
slug: kafka-backed-durable-ordered-chat-delivery-with-outbox-publishing-unified-message-send-flow-and-frontend-reconciliation-optimization
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-29
---

# Phase 11 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Spring Boot Test + JUnit 5, Vitest |
| **Config file** | `backend/src/test/resources/application-test.yml`, `frontend/vitest.config.ts` |
| **Quick run command** | `cd backend && mvn -q -Dtest=ChatMessageCommandServiceTest,ChatOutboxRelayTest,ChatDeliveryConsumerTest test` and `cd frontend && npm test -- --run src/tests/chat-message-ack-reconciliation.test.tsx src/tests/chat-reconnect-catchup.test.tsx` |
| **Full suite command** | `cd backend && mvn -q -Dtest=ChatMessageCommandServiceTest,ChatOutboxRelayTest,ChatDeliveryConsumerTest,ChatControllerTest,ChatWebSocketControllerTest test` and `cd frontend && npm test -- --run src/tests/chat-message-ack-reconciliation.test.tsx src/tests/chat-reconnect-catchup.test.tsx src/tests/chat-realtime-fallback.test.tsx` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd backend && mvn -q -Dtest=ChatMessageCommandServiceTest,ChatOutboxRelayTest,ChatDeliveryConsumerTest test` or `cd frontend && npm test -- --run src/tests/chat-message-ack-reconciliation.test.tsx src/tests/chat-reconnect-catchup.test.tsx` depending on the touched layer
- **After every plan wave:** Run the full suite command
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 11-01-01 | 01 | 1 | durable persistence + unified send | backend unit/integration | `cd backend && mvn -q -Dtest=ChatMessageCommandServiceTest,ChatOutboxRelayTest test` | no | pending |
| 11-02-01 | 02 | 2 | ordered async delivery | backend integration | `cd backend && mvn -q -Dtest=ChatDeliveryConsumerTest,ChatWebSocketControllerTest test` | no | pending |
| 11-03-01 | 03 | 3 | optimistic ack reconciliation | frontend hook/store | `cd frontend && npm test -- --run src/tests/chat-message-ack-reconciliation.test.tsx` | no | pending |
| 11-03-02 | 03 | 3 | reconnect cursor catch-up | frontend hook/store | `cd frontend && npm test -- --run src/tests/chat-reconnect-catchup.test.tsx` | no | pending |
| 11-04-01 | 04 | 4 | end-to-end delivery parity | backend + frontend regression | `cd backend && mvn -q -Dtest=ChatMessageCommandServiceTest,ChatOutboxRelayTest,ChatDeliveryConsumerTest,ChatControllerTest,ChatWebSocketControllerTest test` and `cd frontend && npm test -- --run src/tests/chat-message-ack-reconciliation.test.tsx src/tests/chat-reconnect-catchup.test.tsx src/tests/chat-realtime-fallback.test.tsx` | no | pending |

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/tradingplatform/chat/service/ChatMessageCommandServiceTest.java` - unified send service persistence, outbox write, and persisted ack semantics
- [ ] `backend/src/test/java/com/tradingplatform/chat/integration/ChatOutboxRelayTest.java` - transactional outbox relay and Kafka publication
- [ ] `backend/src/test/java/com/tradingplatform/chat/integration/ChatDeliveryConsumerTest.java` - recipient delivery ordering and async status advancement
- [ ] `frontend/src/tests/chat-message-ack-reconciliation.test.tsx` - optimistic message replacement from persisted ack payload
- [ ] `frontend/src/tests/chat-reconnect-catchup.test.tsx` - reconnect catch-up using persisted cursor markers

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Sender sees fast persisted ack while recipient delivery arrives asynchronously | delivery architecture | Needs browser timing verification across two active sessions | Open two browser sessions, send messages, confirm sender status becomes persisted immediately and recipient receipt still lands without full thread refresh |
| Reconnect catch-up fills only missing messages instead of full thread reload | reconnect recovery | Browser reconnect timing and visual continuity are hard to fully assert in jsdom | Disconnect websocket, send background messages from another session, reconnect, confirm only missing messages append and the active thread does not fully flicker/reload |
| Presence and typing still work while Kafka handles only durable delivery | regression safety | Cross-channel behavior spans Redis presence and Kafka delivery | Keep one user typing and changing presence while sending messages, confirm typing/presence behavior is unchanged after delivery refactor |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
