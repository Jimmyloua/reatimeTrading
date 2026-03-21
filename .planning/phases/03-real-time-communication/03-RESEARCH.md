# Phase 3: Real-Time Communication - Research

**Researched:** 2026-03-22
**Domain:** WebSocket/STOMP messaging, Kafka event streaming, real-time notifications
**Confidence:** HIGH

## Summary

Phase 3 implements real-time communication through WebSocket-based chat with STOMP protocol, message persistence via Kafka and MySQL, and a notification system for user events. The architecture follows the critical requirement from ROADMAP.md: messages must be written to database before WebSocket delivery to prevent message loss on server restart.

The implementation uses Spring WebSocket with STOMP for bidirectional communication, Apache Kafka for durable message queuing, Redis pub/sub for multi-instance message distribution, and MySQL for persistent storage. The frontend uses @stomp/stompjs 7.x with sockjs-client for WebSocket connections with fallback support.

**Primary recommendation:** Implement a three-tier message flow: (1) Client sends message via WebSocket, (2) Server persists to MySQL via Kafka topic, (3) Server delivers to recipient via WebSocket with Redis pub/sub for cluster distribution. This ensures message durability while maintaining real-time UX.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Users can initiate chats from both item detail page ("Contact Seller" button) AND directly from user profiles (direct messaging)
- **D-02:** Each item creates a separate conversation thread with the same user (not merged into single thread)
- **D-03:** Item context shown as clickable link in chat header (no price snapshot or title snapshot)
- **D-04:** Clicking item link opens item detail in new tab
- **D-05:** Messages auto-deleted after 90 days (time-limited retention)
- **D-06:** Users see read receipts (message status: sent, delivered, read)
- **D-07:** Image sharing supported in chat (images only for v1, no documents/files)
- **D-08:** Max image size: 5MB per image (consistent with avatar limit from Phase 1)
- **D-09:** Typing indicator shown ("User is typing...") in active conversations
- **D-10:** Online/offline presence shown per user ("online" or "last seen X ago")
- **D-11:** Presence status visible in chat header and conversation list
- **D-12:** Real-time WebSocket push only for v1 (no email fallback, no browser push notifications)
- **D-13:** Notification history limited to 50 unread, retained for 30 days
- **D-14:** Notification types: new messages, item sold, transaction updates, system announcements, payment status changes
- **D-15:** Users can mark notifications as read (supports NOTF-04)
- **D-16:** Dedicated /messages page with conversation list and chat view (not floating widget)
- **D-17:** Conversations ordered by most recent message (most recent at top)
- **D-18:** Unread message count shown on conversation list items

### Claude's Discretion
- Exact WebSocket reconnection strategy (exponential backoff, max retries)
- Message pagination for chat history (number of messages per page)
- Typing indicator debounce timing (how long after stop typing)
- Presence heartbeat interval (how often to check online status)
- Notification grouping/batching strategy
- Image upload flow (drag-drop, click to upload, preview before send)

### Deferred Ideas (OUT OF SCOPE)
- Structured Trading Actions (Phase 4): "Make offer" button in chat, "Accept" / "Reject" / "Counter" offer actions, "Confirm payment" action
- Payment Notifications (Phase 4): "Your item is on pay" notification, "Payment cancelled" notification
- Future Enhancements (v2+): Email fallback for offline users, browser push notifications, document/file sharing in chat, message editing and deletion, voice/video calling
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CHAT-01 | User can initiate chat with seller about an item | Conversation entity with Listing FK, ChatController endpoint, "Contact Seller" button in UI |
| CHAT-02 | User can send and receive real-time messages | STOMP WebSocket endpoints, MessageController, @stomp/stompjs client |
| CHAT-03 | User can view chat history with other users | MessageRepository with pagination, GET /api/conversations/{id}/messages endpoint |
| CHAT-04 | User receives message persistence (messages stored in database) | MySQL Message entity, Kafka topic for durability, ROADMAP critical note on DB-first write |
| CHAT-05 | User can see when other party is typing (optional presence indicator) | STOMP typing topic subscription, debounced frontend emission, 3-second timeout |
| NOTF-01 | User receives real-time notification when receiving a message | NotificationService, STOMP /user/queue/notifications subscription |
| NOTF-02 | User receives notification when item sells | TransactionEvent listener, Notification entity with ITEM_SOLD type |
| NOTF-03 | User can view notification history | NotificationRepository, GET /api/notifications endpoint with pagination |
| NOTF-04 | User can mark notifications as read | PATCH /api/notifications/{id}/read endpoint, Notification.markAsRead() |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| spring-boot-starter-websocket | 3.4.2 (BOM) | WebSocket support | Native Spring integration, STOMP protocol support, SimpMessagingTemplate for server-push |
| spring-kafka | 3.3.x (BOM) | Message queue | Durable message persistence, event sourcing, async processing, replay capability |
| @stomp/stompjs | 7.3.0 | WebSocket client | Full STOMP protocol, auto-reconnect, TypeScript support, active maintenance |
| sockjs-client | 1.6.1 | WebSocket fallback | XHR streaming, iframe fallback for restricted networks, Spring SockJS support |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| spring-boot-starter-data-redis | 3.4.2 (BOM) | Pub/sub for cluster | Multi-instance message distribution, presence tracking |
| spring-session-data-redis | 3.4.2 (BOM) | Distributed sessions | Already configured; WebSocket session integration |
| spring-boot-starter-data-jpa | 3.4.2 (BOM) | Message persistence | ChatMessage and Conversation entities |
| spring-boot-starter-security | 3.4.2 (BOM) | WebSocket auth | JWT authentication on STOMP CONNECT frame |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| STOMP over WebSocket | Server-Sent Events (SSE) | SSE is one-way only; chat requires bidirectional. SSE simpler for notifications-only. |
| STOMP over WebSocket | Raw WebSocket | Raw WebSocket requires custom protocol; STOMP provides subscribe/publish model, message acknowledgment. |
| Kafka for persistence | Direct DB writes | Kafka provides durability, replay, async processing. Direct DB is simpler but no built-in retry/replay. |
| Kafka for persistence | RabbitMQ | RabbitMQ is simpler for queues; Kafka better for event sourcing and replay. Project mandates Kafka. |
| sockjs-client | Native WebSocket only | SockJS adds 15KB but provides fallbacks for corporate firewalls. Recommended for production. |

**Installation:**

Backend (add to pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Frontend (add to package.json):
```bash
npm install @stomp/stompjs sockjs-client
npm install -D @types/sockjs-client
```

**Version verification:**
- @stomp/stompjs: 7.3.0 (verified 2026-03-22 via npm view)
- sockjs-client: 1.6.1 (verified 2026-03-22 via npm view)
- Spring Boot: 3.4.2 (verified from project pom.xml)

## Architecture Patterns

### Recommended Project Structure

```
backend/src/main/java/com/tradingplatform/
├── chat/
│   ├── controller/
│   │   ├── ChatController.java          # REST endpoints for chat history
│   │   └── ChatWebSocketController.java # STOMP message handlers
│   ├── dto/
│   │   ├── SendMessageRequest.java
│   │   ├── MessageResponse.java
│   │   └── ConversationResponse.java
│   ├── entity/
│   │   ├── Conversation.java            # Chat thread between 2 users about an item
│   │   ├── ChatMessage.java             # Individual message
│   │   └── MessageStatus.java           # SENT, DELIVERED, READ
│   ├── repository/
│   │   ├── ConversationRepository.java
│   │   └── MessageRepository.java
│   ├── service/
│   │   ├── ChatService.java             # Business logic
│   │   ├── MessagePersistenceService.java
│   │   └── PresenceService.java         # Online/offline tracking
│   └── config/
│       └── WebSocketConfig.java         # STOMP broker config
├── notification/
│   ├── controller/
│   │   ├── NotificationController.java
│   │   └── NotificationWebSocketHandler.java
│   ├── dto/
│   │   └── NotificationResponse.java
│   ├── entity/
│   │   ├── Notification.java
│   │   └── NotificationType.java
│   ├── repository/
│   │   └── NotificationRepository.java
│   └── service/
│       └── NotificationService.java
├── config/
│   ├── WebSocketSecurityConfig.java     # WebSocket authentication
│   └── KafkaConfig.java                 # Kafka producer/consumer config

frontend/src/
├── api/
│   ├── chatApi.ts                       # REST endpoints for history
│   └── notificationApi.ts
├── hooks/
│   ├── useWebSocket.ts                  # STOMP connection management
│   ├── useChat.ts                       # Message sending/receiving
│   └── useNotifications.ts
├── stores/
│   ├── chatStore.ts                     # Zustand for chat UI state
│   └── notificationStore.ts
├── components/
│   ├── chat/
│   │   ├── ConversationList.tsx
│   │   ├── ChatView.tsx
│   │   ├── MessageBubble.tsx
│   │   ├── MessageInput.tsx
│   │   └── TypingIndicator.tsx
│   └── notifications/
│       ├── NotificationList.tsx
│       └── NotificationItem.tsx
└── pages/
    └── MessagesPage.tsx                 # /messages route
```

### Pattern 1: WebSocket Configuration with JWT Authentication

**What:** Configure STOMP WebSocket endpoint with JWT authentication on CONNECT frame.

**When to use:** All WebSocket connections requiring user authentication.

**Example:**
```java
// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for /topic and /queue destinations
        // Use Redis for multi-instance deployment
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("http://localhost:*")
            .withSockJS();  // Fallback support
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preMessage(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        // Validate JWT and set authentication
                        Authentication auth = jwtTokenProvider.getAuthentication(token);
                        accessor.setUser(auth);
                    }
                }
                return message;
            }
        });
    }
}
```

### Pattern 2: Database-First Message Persistence

**What:** Write messages to database via Kafka before delivering to recipient via WebSocket.

**When to use:** All chat messages to prevent data loss on server restart.

**Example:**
```java
// ChatWebSocketController.java
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        Long senderId = ((UserPrincipal) principal).getUserId();

        // 1. Persist to database (synchronous for ACK)
        ChatMessage message = chatService.saveMessage(request, senderId);

        // 2. Publish to Kafka for async processing
        chatService.publishMessageEvent(message);

        // 3. Deliver to recipient via WebSocket
        messagingTemplate.convertAndSendToUser(
            request.getRecipientId().toString(),
            "/queue/messages",
            MessageResponse.from(message)
        );

        // 4. Send ACK back to sender
        messagingTemplate.convertAndSendToUser(
            senderId.toString(),
            "/queue/message-ack",
            new MessageAck(message.getId(), MessageStatus.DELIVERED)
        );
    }
}
```

### Pattern 3: Typing Indicator with Debounce

**What:** Send typing events with client-side debounce to avoid flooding.

**When to use:** Real-time typing indicators in chat.

**Example:**
```java
// Backend - ChatWebSocketController.java
@MessageMapping("/chat.typing")
public void handleTyping(@Payload TypingRequest request, Principal principal) {
    // Broadcast to conversation participants
    messagingTemplate.convertAndSend(
        "/topic/conversation." + request.getConversationId() + ".typing",
        new TypingResponse(principal.getName(), request.isTyping())
    );
}
```

```typescript
// Frontend - useChat.ts
const TYPING_DEBOUNCE_MS = 3000;
let typingTimeout: NodeJS.Timeout | null = null;

const emitTyping = useCallback(() => {
  if (typingTimeout) clearTimeout(typingTimeout);

  stompClient.publish({
    destination: `/app/chat.typing`,
    body: JSON.stringify({ conversationId, typing: true })
  });

  typingTimeout = setTimeout(() => {
    stompClient.publish({
      destination: `/app/chat.typing`,
      body: JSON.stringify({ conversationId, typing: false })
    });
  }, TYPING_DEBOUNCE_MS);
}, [conversationId, stompClient]);
```

### Pattern 4: Frontend WebSocket Connection with Auto-Reconnect

**What:** STOMP client with exponential backoff reconnection.

**When to use:** All WebSocket clients in production.

**Example:**
```typescript
// hooks/useWebSocket.ts
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket() {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const reconnectAttempts = useRef(0);
  const MAX_RECONNECT_DELAY = 30000; // 30 seconds
  const BASE_DELAY = 1000; // 1 second

  const connect = useCallback(() => {
    const token = useAuthStore.getState().accessToken;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        setConnected(true);
        reconnectAttempts.current = 0;
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error('STOMP error:', frame),
      onWebSocketClose: () => {
        // Exponential backoff reconnect
        const delay = Math.min(
          BASE_DELAY * Math.pow(2, reconnectAttempts.current),
          MAX_RECONNECT_DELAY
        );
        reconnectAttempts.current++;
        setTimeout(connect, delay);
      },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.activate();
    clientRef.current = client;
  }, []);

  return { client: clientRef.current, connected, connect };
}
```

### Pattern 5: Conversation Entity Design

**What:** Separate conversation threads per item, not merged by user pair.

**When to use:** When users can negotiate about multiple items with the same person.

**Example:**
```java
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conv_item", columnList = "listing_id"),
    @Index(name = "idx_conv_participants", columnList = "buyer_id, seller_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;  // FK to Listing

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;    // User who initiated chat

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;   // Owner of listing

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "buyer_unread_count")
    @Builder.Default
    private Integer buyerUnreadCount = 0;

    @Column(name = "seller_unread_count")
    @Builder.Default
    private Integer sellerUnreadCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper to get the other participant
    public Long getOtherParticipantId(Long currentUserId) {
        return currentUserId.equals(buyerId) ? sellerId : buyerId;
    }
}
```

### Anti-Patterns to Avoid

- **Sending messages before persistence:** Must write to DB first (ROADMAP critical note). If server crashes after WebSocket send but before DB write, message is lost.
- **Blocking WebSocket threads with DB operations:** Use @Async or virtual threads for DB writes after initial persistence ACK.
- **Storing full images in database:** Store in S3/filesystem, save only URLs. Same pattern as Listing images.
- **Subscribing to all conversations globally:** Subscribe only to active conversation to reduce server load.
- **Polling for typing indicators:** Use STOMP broadcast, not REST polling.
- **Single WebSocket connection per user:** User may have multiple tabs; design for multiple connections per user.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| WebSocket authentication | Custom token validation on each message | Spring Security with ChannelInterceptor | Spring provides built-in authentication propagation, session management |
| Message ordering | Custom sequence numbers | Kafka partitions with key-based routing | Kafka guarantees ordering within partition, handles retries |
| Online presence | Custom heartbeat implementation | Redis with TTL keys + WebSocket heartbeat | Redis handles TTL expiration, distributed across instances |
| Reconnection logic | Custom retry loops | @stomp/stompjs built-in reconnect with backoff | Handles edge cases (network flaps, server restarts) |
| Message status tracking | Custom state machine | Database enum with service layer | Simpler, auditable, queryable for UI |
| Typing indicator debouncing | Client-side setTimeout cascade | useDebouncedCallback from usehooks-ts | Handles cleanup, prevents memory leaks |
| Image upload in chat | Custom multipart handling | Reuse ListingImageService pattern | Already implemented and tested |

**Key insight:** WebSocket real-time messaging is deceptively complex. The STOMP protocol and Spring's WebSocket support handle edge cases (heartbeats, frame parsing, subscriptions) that are easy to get wrong with raw WebSocket.

## Common Pitfalls

### Pitfall 1: Message Loss on Server Restart
**What goes wrong:** Message sent via WebSocket but server crashes before database write. Message is lost forever.
**Why it happens:** WebSocket is ephemeral; no persistence without explicit storage.
**How to avoid:** ROADMAP.md critical note: write to database BEFORE WebSocket delivery. Use synchronous DB write in message handler, then deliver to recipient.
**Warning signs:** Messages appear sent but never in history after server restart.

### Pitfall 2: WebSocket Authentication Bypass
**What goes wrong:** WebSocket endpoint allows unauthenticated connections, exposing all subscriptions.
**Why it happens:** WebSocket configuration is separate from HTTP SecurityConfig.
**How to avoid:** Configure ChannelInterceptor to validate JWT on STOMP CONNECT frame. Use Spring Security's WebSocketSecurityConfig.
**Warning signs:** Can connect to WebSocket without valid token; can subscribe to any user's queue.

### Pitfall 3: Cross-Instance Message Delivery Failure
**What goes wrong:** User A connects to instance 1, User B connects to instance 2. Messages don't arrive.
**Why it happens:** Simple in-memory broker doesn't share subscriptions across instances.
**How to avoid:** Use Redis pub/sub for message distribution. Configure `config.enableSimpleBroker()` with Redis-backed message broker, or use `spring-boot-starter-data-redis` with `ChannelTopic`.
**Warning signs:** Messages work in dev (single instance) but fail in production (multiple instances).

### Pitfall 4: Typing Indicator Flooding
**What goes wrong:** Every keystroke sends a WebSocket message, overwhelming the server.
**Why it happens:** Naive implementation hooks directly to onChange event.
**How to avoid:** Debounce on client (3000ms timeout) before sending. Clear timeout on each keystroke.
**Warning signs:** High CPU usage on server when user types quickly.

### Pitfall 5: Memory Leak from Unsubscribed Topics
**What goes wrong:** User navigates away from chat but subscription remains active, causing memory leaks and unnecessary server load.
**Why it happens:** React component unmounts without cleanup.
**How to avoid:** Use useEffect cleanup to unsubscribe. Store subscription references and call `.unsubscribe()` on unmount.
**Warning signs:** Browser memory grows over time; server has many zombie subscriptions.

### Pitfall 6: Orphaned Conversations from Deleted Listings
**What goes wrong:** Listing is deleted, but conversations about it remain with broken references.
**Why it happens:** No cascade handling for Listing deletion.
**How to avoid:** Use soft delete on Listings (already implemented with `is_deleted`). Keep conversation history accessible. Consider showing "This item is no longer available" in chat header.
**Warning signs:** Foreign key constraint errors or broken item links in chat.

## Code Examples

### Complete ChatMessage Entity

```java
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_msg_conversation", columnList = "conversation_id"),
    @Index(name = "idx_msg_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;  // Optional image attachment

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

public enum MessageStatus {
    SENT,       // Saved to database
    DELIVERED,  // Received by recipient's device
    READ        // Recipient opened the message
}
```

### Frontend Chat Store (Zustand)

```typescript
// stores/chatStore.ts
import { create } from 'zustand';
import type { Message, Conversation } from '@/types/chat';

interface ChatState {
  conversations: Conversation[];
  activeConversation: Conversation | null;
  messages: Message[];
  typingUsers: Map<number, boolean>;  // userId -> isTyping

  setConversations: (conversations: Conversation[]) => void;
  setActiveConversation: (conversation: Conversation | null) => void;
  addMessage: (message: Message) => void;
  prependMessages: (messages: Message[]) => void;  // For pagination
  setTyping: (userId: number, isTyping: boolean) => void;
  incrementUnread: (conversationId: number) => void;
  clearUnread: (conversationId: number) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  conversations: [],
  activeConversation: null,
  messages: [],
  typingUsers: new Map(),

  setConversations: (conversations) => set({ conversations }),
  setActiveConversation: (conversation) => set({
    activeConversation: conversation,
    messages: []  // Clear messages when switching conversations
  }),
  addMessage: (message) => set((state) => ({
    messages: [...state.messages, message]
  })),
  prependMessages: (messages) => set((state) => ({
    messages: [...messages, ...state.messages]
  })),
  setTyping: (userId, isTyping) => set((state) => {
    const newTyping = new Map(state.typingUsers);
    newTyping.set(userId, isTyping);
    return { typingUsers: newTyping };
  }),
  incrementUnread: (conversationId) => set((state) => ({
    conversations: state.conversations.map(c =>
      c.id === conversationId
        ? { ...c, unreadCount: (c.unreadCount || 0) + 1 }
        : c
    )
  })),
  clearUnread: (conversationId) => set((state) => ({
    conversations: state.conversations.map(c =>
      c.id === conversationId ? { ...c, unreadCount: 0 } : c
    )
  }))
}));
```

### Notification Service with WebSocket Push

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createNotification(Long userId, NotificationType type,
                                           String title, String content, Long referenceId) {
        Notification notification = Notification.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .content(content)
            .referenceId(referenceId)
            .read(false)
            .build();

        notification = notificationRepository.save(notification);

        // Push via WebSocket
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            NotificationResponse.from(notification)
        );

        return notification;
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
            .ifPresent(n -> {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
            });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Raw WebSocket with custom protocol | STOMP over WebSocket | Standard (Spring 4.x+) | Structured subscribe/publish model, message acknowledgment |
| In-memory message broker | Redis pub/sub for cluster | Spring 4.2+ | Multi-instance support, horizontal scaling |
| Polling for online status | WebSocket heartbeat + Redis TTL | Modern standard | Real-time accuracy, reduced server load |
| Synchronous message processing | Kafka for async persistence | Spring Boot 2.x+ | Durability, replay capability, async processing |

**Deprecated/outdated:**
- **Spring WebSocket with XML configuration:** Use Java config with `@EnableWebSocketMessageBroker`.
- **SockJS without STOMP:** Raw SockJS is lower-level; STOMP provides structured messaging.
- **Polling for messages:** Always use WebSocket push for real-time chat.

## Open Questions

1. **Kafka Topic Design**
   - What we know: Single topic `chat-messages` with partition key by conversationId for ordering.
   - What's unclear: Whether to use separate topics for notifications vs chat messages.
   - Recommendation: Single topic with message type field for simplicity. Separate topics if different retention policies needed.

2. **Message Retention Implementation**
   - What we know: 90-day retention for messages (D-05).
   - What's unclear: Whether to use MySQL `ON DELETE` with scheduled job or Kafka retention.
   - Recommendation: MySQL with scheduled cleanup job. Kafka retention is time-based but doesn't delete specific records.

3. **Image Upload Flow**
   - What we know: Max 5MB per image (D-08), same limit as avatar.
   - What's unclear: Whether to reuse `ListingStorageService` pattern or create separate chat image storage.
   - Recommendation: Reuse existing storage service pattern with separate subdirectory `/uploads/chat-images/`.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test (backend), Vitest + Testing Library (frontend) |
| Config file | `application-test.yml` (backend), `vitest.config.ts` (frontend) |
| Quick run command (backend) | `mvn test -Dtest=ChatControllerTest` |
| Quick run command (frontend) | `npm test -- --grep "chat"` |
| Full suite command (backend) | `mvn test` |
| Full suite command (frontend) | `npm test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| CHAT-01 | Initiate chat with seller | integration | `mvn test -Dtest=ChatControllerTest#testCreateConversation` | Wave 0 |
| CHAT-02 | Send/receive real-time messages | integration | `mvn test -Dtest=ChatWebSocketControllerTest` | Wave 0 |
| CHAT-03 | View chat history | unit | `mvn test -Dtest=MessageRepositoryTest#testFindByConversationId` | Wave 0 |
| CHAT-04 | Message persistence | integration | `mvn test -Dtest=ChatServiceTest#testMessagePersistedBeforeDelivery` | Wave 0 |
| CHAT-05 | Typing indicator | integration | `mvn test -Dtest=TypingIndicatorTest` | Wave 0 |
| NOTF-01 | Real-time notification on message | integration | `mvn test -Dtest=NotificationServiceTest#testMessageNotification` | Wave 0 |
| NOTF-02 | Notification when item sells | integration | `mvn test -Dtest=NotificationServiceTest#testItemSoldNotification` | Wave 0 |
| NOTF-03 | View notification history | unit | `mvn test -Dtest=NotificationRepositoryTest` | Wave 0 |
| NOTF-04 | Mark notifications as read | unit | `mvn test -Dtest=NotificationServiceTest#testMarkAsRead` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest={affected test class}` or `npm test -- --grep {pattern}`
- **Per wave merge:** `mvn test` (full backend suite)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/java/com/tradingplatform/chat/controller/ChatControllerTest.java` - covers CHAT-01, CHAT-03
- [ ] `backend/src/test/java/com/tradingplatform/chat/controller/ChatWebSocketControllerTest.java` - covers CHAT-02, CHAT-05
- [ ] `backend/src/test/java/com/tradingplatform/chat/service/ChatServiceTest.java` - covers CHAT-04
- [ ] `backend/src/test/java/com/tradingplatform/chat/repository/MessageRepositoryTest.java` - covers CHAT-03
- [ ] `backend/src/test/java/com/tradingplatform/notification/controller/NotificationControllerTest.java` - covers NOTF-03, NOTF-04
- [ ] `backend/src/test/java/com/tradingplatform/notification/service/NotificationServiceTest.java` - covers NOTF-01, NOTF-02, NOTF-04
- [ ] `frontend/src/tests/chat.test.tsx` - UI tests for chat components
- [ ] `frontend/src/tests/notifications.test.tsx` - UI tests for notification components
- [ ] Maven dependency additions: spring-boot-starter-websocket, spring-kafka
- [ ] npm dependency additions: @stomp/stompjs, sockjs-client, @types/sockjs-client

## Sources

### Primary (HIGH confidence)
- Spring Framework Reference Documentation - WebSocket support, STOMP configuration
- npm Registry - @stomp/stompjs 7.3.0, sockjs-client 1.6.1 verified 2026-03-22
- Project pom.xml - Spring Boot 3.4.2, existing dependency versions
- CLAUDE.md - Mandated tech stack (Spring Boot, React, MySQL, Redis, Kafka)

### Secondary (MEDIUM confidence)
- Spring WebSocket documentation patterns for JWT authentication
- Existing project patterns: User entity, Listing entity, SecurityConfig, API client pattern

### Tertiary (LOW confidence)
- None - all recommendations verified against project context or official sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All versions verified from package registries, Spring BOM versions from project pom.xml
- Architecture: HIGH - Patterns follow Spring documentation and established project conventions
- Pitfalls: HIGH - Based on documented ROADMAP critical notes and common WebSocket issues

**Research date:** 2026-03-22
**Valid until:** 30 days (stable Spring/React ecosystem)