---
phase: 03-real-time-communication
plan: 04
subsystem: frontend-chat-ui
tags: [websocket, chat, stomp, react, zustand]
requires: [03-03]
provides: [chat-ui, websocket-client, notification-ui]
affects: [frontend]
tech_stack:
  added:
    - "@stomp/stompjs@7.x"
    - "sockjs-client@1.6.x"
    - "shadcn badge, scroll-area, textarea"
  patterns:
    - "STOMP over WebSocket with auto-reconnect"
    - "Zustand for chat and notification state"
    - "Two-column chat layout"
key_files:
  created:
    - frontend/src/types/chat.ts
    - frontend/src/types/notification.ts
    - frontend/src/hooks/useWebSocket.ts
    - frontend/src/hooks/useChat.ts
    - frontend/src/hooks/useNotifications.ts
    - frontend/src/stores/chatStore.ts
    - frontend/src/stores/notificationStore.ts
    - frontend/src/api/chatApi.ts
    - frontend/src/api/notificationApi.ts
    - frontend/src/components/chat/ConversationItem.tsx
    - frontend/src/components/chat/ConversationList.tsx
    - frontend/src/components/chat/ChatView.tsx
    - frontend/src/components/chat/MessageBubble.tsx
    - frontend/src/components/chat/MessageInput.tsx
    - frontend/src/components/chat/TypingIndicator.tsx
    - frontend/src/pages/MessagesPage.tsx
  modified:
    - frontend/src/App.tsx
decisions:
  - "Exponential backoff for WebSocket reconnection (1s base, 30s max, 5 attempts)"
  - "3-second typing indicator debounce"
  - "Two-column layout with 280px fixed sidebar and flexible chat view"
  - "Message input with Enter to send, Shift+Enter for newline"
metrics:
  duration: 12 minutes
  tasks: 3
  files_created: 16
  files_modified: 1
---

# Phase 03 Plan 04: Frontend WebSocket Client Summary

## One-liner

Frontend chat UI with STOMP WebSocket client, Zustand state management, conversation list, and real-time messaging components.

## Changes Made

### Task 1: WebSocket Hook and Type Definitions

Created TypeScript types for chat and notifications, and WebSocket hook with STOMP client:

- `types/chat.ts`: Message, Conversation, TypingIndicator interfaces
- `types/notification.ts`: Notification interface with all notification types
- `useWebSocket.ts`: STOMP client with auto-reconnect, exponential backoff, connection state management
- `useChat.ts`: Chat hook with message sending and typing indicator emission

### Task 2: Chat Store and API Functions

Created Zustand store and REST API functions for chat:

- `chatStore.ts`: Conversation and message state, typing users tracking, unread count management
- `chatApi.ts`: createConversation, getConversations, getMessages REST endpoints

### Task 3: MessagesPage with Chat UI Components

Created complete chat UI with two-column layout:

- `ConversationItem.tsx`: Individual conversation preview with avatar, unread badge
- `ConversationList.tsx`: Scrollable list with loading and empty states
- `MessageBubble.tsx`: Message display with sent/received styling
- `TypingIndicator.tsx`: Shows typing users
- `MessageInput.tsx`: Textarea with send button and image placeholder
- `ChatView.tsx`: Full chat view with header, messages, typing indicator, input
- `MessagesPage.tsx`: Two-column layout (280px sidebar + flexible chat view)
- Updated `App.tsx` with `/messages` route

### Additional: Notification Infrastructure

Created notification support files for future use:

- `notificationStore.ts`: Zustand store for notifications
- `notificationApi.ts`: REST API for notifications
- `useNotifications.ts`: Hook for real-time notification subscription

## Deviations from Plan

None - plan executed exactly as written.

## Technical Details

### WebSocket Configuration

- Connection: SockJS with STOMP protocol
- Endpoint: `/ws` (connects to backend WebSocket)
- Authentication: JWT Bearer token in STOMP CONNECT headers
- Reconnection: Exponential backoff (1s base, doubled each attempt, max 30s, 5 attempts)
- Heartbeat: 10 seconds in/out

### Chat Store Structure

```typescript
interface ChatState {
  conversations: Conversation[]
  activeConversation: Conversation | null
  messages: Message[]
  typingUsers: Map<number, boolean>
  isLoading: boolean
  error: string | null
}
```

### UI Layout

- Desktop: Two-column layout (280px sidebar + 1fr chat view)
- Conversation list shows avatar, name, item title, unread badge, timestamp
- Chat view shows header with user name and item link, message list, typing indicator, input

## Files Not Implemented

- Image upload in chat (button present but not wired - deferred to future enhancement)
- Mobile responsive layout (desktop-focused for v1)

## Commits

| Commit | Message |
|--------|---------|
| 4d430e53 | feat(03-04): add WebSocket hook and type definitions |
| ccbbb13c | feat(03-04): add chat store and API functions |
| 0f9e9ad8 | feat(03-04): add MessagesPage with ConversationList and ChatView components |
| 6bbd8f6b | feat(03-04): add notification store, API, and hook |

## Requirements Addressed

- CHAT-01: User can initiate chat with seller (API and UI ready)
- CHAT-02: User can send and receive real-time messages (WebSocket client implemented)
- CHAT-03: User can view chat history (MessagesPage with conversation list)
- CHAT-05: User sees typing indicator (TypingIndicator component, useChat hook)

---

*Plan completed: 2026-03-22*