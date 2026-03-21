---
phase: 03-real-time-communication
plan: 01
subsystem: chat
tags: [chat, entities, rest-api, persistence-first]
dependencies:
  requires: [03-00]
  provides: [CHAT-01, CHAT-03, CHAT-04]
  affects: [chat-module, listing-module, user-module]
tech_stack:
  added:
    - Spring Data JPA for ChatMessage and Conversation
    - MapStruct for DTO mapping
  patterns:
    - Persistence-first message saving (ROADMAP critical note)
    - JPA entities with auditing
    - Repository pattern with derived queries
key_files:
  created:
    - backend/src/main/java/com/tradingplatform/chat/entity/Conversation.java
    - backend/src/main/java/com/tradingplatform/chat/entity/ChatMessage.java
    - backend/src/main/java/com/tradingplatform/chat/entity/MessageStatus.java
    - backend/src/main/java/com/tradingplatform/chat/repository/ConversationRepository.java
    - backend/src/main/java/com/tradingplatform/chat/repository/MessageRepository.java
    - backend/src/main/java/com/tradingplatform/chat/dto/CreateConversationRequest.java
    - backend/src/main/java/com/tradingplatform/chat/dto/SendMessageRequest.java
    - backend/src/main/java/com/tradingplatform/chat/dto/ConversationResponse.java
    - backend/src/main/java/com/tradingplatform/chat/dto/MessageResponse.java
    - backend/src/main/java/com/tradingplatform/chat/mapper/ChatMapper.java
    - backend/src/main/java/com/tradingplatform/chat/service/ChatService.java
    - backend/src/main/java/com/tradingplatform/chat/controller/ChatController.java
    - backend/src/main/resources/db/changelog/006-create-chat-tables.xml
  modified:
    - backend/src/main/java/com/tradingplatform/exception/ErrorCode.java
    - backend/src/main/resources/db/changelog/db.changelog-master.xml
decisions:
  - key: "D-02"
    summary: "Each item creates a separate conversation thread (not merged)"
    impact: "Conversation entity has listingId FK, unique constraint on (listingId, buyerId)"
  - key: "D-06"
    summary: "Read receipts with SENT, DELIVERED, READ status"
    impact: "MessageStatus enum and status field on ChatMessage"
  - key: "D-17"
    summary: "Conversations ordered by most recent message"
    impact: "lastMessageAt field and index for ordering"
  - key: "D-18"
    summary: "Unread message count on conversation list"
    impact: "buyerUnreadCount and sellerUnreadCount fields"
  - key: "ROADMAP"
    summary: "Messages written to DB before WebSocket delivery"
    impact: "ChatService.sendMessage() calls messageRepository.save() first"
metrics:
  duration_minutes: 36
  task_count: 3
  files_created: 13
  files_modified: 2
  tests_added: 27
  test_pass_rate: 100
---

# Phase 03 Plan 01: Chat Entities and REST API Summary

## One-Liner

Chat backend with Conversation and ChatMessage entities, persistence-first message saving, and REST API endpoints for initiating conversations and viewing history.

## Requirements Delivered

| ID | Description | Status |
|----|-------------|--------|
| CHAT-01 | User can initiate chat with seller about an item | Delivered |
| CHAT-03 | User can view chat history with other users | Delivered |
| CHAT-04 | Messages are persisted to database | Delivered |

## Implementation Details

### Task 1: Chat Entities and Repositories

Created JPA entities following established patterns:

- **Conversation** - Chat thread between buyer and seller about a listing
  - Implements D-02: Separate thread per item via `listingId` FK
  - Implements D-17: `lastMessageAt` for ordering
  - Implements D-18: `buyerUnreadCount` and `sellerUnreadCount` for unread badges
  - Helper methods: `getOtherParticipantId()`, `getUnreadCountForUser()`

- **ChatMessage** - Individual message in a conversation
  - Implements D-06: `MessageStatus` enum (SENT, DELIVERED, READ)
  - Implements D-07: `imageUrl` for image sharing support

- **Repositories** with derived queries:
  - `findByListingIdAndBuyerId` - Check for existing conversation
  - `findByParticipantId` - Get user's conversations ordered by lastMessageAt
  - `findByConversationIdOrderByCreatedAtDesc` - Paginated message history
  - `markAsRead` - Bulk update for read receipts

- **Liquibase migration** 006-create-chat-tables.xml with proper indexes

### Task 2: DTOs and Mapper

Created request/response DTOs with validation:

- `CreateConversationRequest` - Validates listingId is required and positive
- `SendMessageRequest` - Supports content and/or imageUrl
- `ConversationResponse` - Includes listingTitle (D-03), otherUserId, unreadCount
- `MessageResponse` - Includes status (D-06), isOwnMessage flag

ChatMapper uses MapStruct with expression mappings for dynamic fields.

### Task 3: ChatService and ChatController

**ChatService** implements critical persistence-first approach:

```java
// CRITICAL: Persist to database FIRST (per ROADMAP note)
message = messageRepository.save(message);
// Then update conversation metadata
```

Key features:
- Creates new conversation or returns existing one
- Verifies participant ownership before operations
- Updates unread counts on message send
- Marks messages as read when fetching history

**ChatController** exposes REST endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/conversations` | POST | Create/initiate chat (CHAT-01) |
| `/api/conversations` | GET | List user's conversations |
| `/api/conversations/{id}` | GET | Get single conversation |
| `/api/conversations/{id}/messages` | GET | View history (CHAT-03) |
| `/api/conversations/{id}/messages` | POST | Send message |
| `/api/conversations/{id}/read` | POST | Mark as read |

## Test Coverage

| Test Class | Tests | Status |
|------------|-------|--------|
| MessageRepositoryTest | 11 | All pass |
| ChatServiceTest | 9 | All pass |
| ChatControllerTest | 7 | All pass |
| **Total** | **27** | **100% pass** |

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - all functionality is fully implemented.

## Next Steps

Plan 03-02 adds Notification backend for real-time message notifications.

---

*Completed: 2026-03-22*
*Commits: a46b9cf3, 8cca1120, 9c61555d*