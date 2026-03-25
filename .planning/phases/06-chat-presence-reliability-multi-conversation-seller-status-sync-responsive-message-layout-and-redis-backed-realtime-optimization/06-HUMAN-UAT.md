---
status: partial
phase: 06-chat-presence-reliability-multi-conversation-seller-status-sync-responsive-message-layout-and-redis-backed-realtime-optimization
source:
  - 06-VERIFICATION.md
started: 2026-03-25T03:18:00Z
updated: 2026-03-25T03:18:00Z
---

## Current Test

approved via execute-phase human verification gate

## Tests

### 1. Reconnect presence separation
expected: On `/messages`, transport reconnect state appears separately from seller presence, seller state stays last-known for about 30 seconds, and matching seller rows/header stay visually synchronized.
result: approved

### 2. Mobile shell and sticky composer
expected: At mobile width, opening a thread replaces the list in-place, a visible `Back to conversations` control returns to the list, the composer stays sticky, and long content does not cause horizontal scrolling.
result: approved

## Summary

total: 2
passed: 2
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

None.
