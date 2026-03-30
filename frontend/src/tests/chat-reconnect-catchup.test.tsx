import { render, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { ChatView } from '@/components/chat/ChatView'
import { useAuthStore } from '@/stores/authStore'
import { useChatStore } from '@/stores/chatStore'
import { chatApi } from '@/api/chatApi'
import type { Conversation, Message } from '@/types/chat'

const websocketMock = vi.hoisted(() => {
  let connectionState: 'reconnecting' | 'connected' = 'reconnecting'

  return {
    subscribe: vi.fn(() => ({ unsubscribe: vi.fn() })),
    publish: vi.fn(),
    getConnectionState() {
      return connectionState
    },
    setConnectionState(nextState: 'reconnecting' | 'connected') {
      connectionState = nextState
    },
    reset() {
      connectionState = 'reconnecting'
      this.subscribe.mockClear()
      this.publish.mockClear()
    },
  }
})

vi.mock('@/hooks/useWebSocket', () => ({
  useWebSocket: () => ({
    subscribe: websocketMock.subscribe,
    publish: websocketMock.publish,
    connectionState: websocketMock.getConnectionState(),
  }),
}))

vi.mock('@/hooks/useConversationPresence', () => ({
  useConversationPresence: () => ({
    isOnline: true,
    lastSeenText: 'Last seen 1m ago',
    status: 'online',
    statusCopy: 'Seller online',
    statusDotClassName: 'bg-emerald-500',
    consumeOnlineToast: () => false,
  }),
}))

vi.mock('@/api/chatApi', () => ({
  chatApi: {
    getConversation: vi.fn(),
    getConversations: vi.fn(),
    getMessages: vi.fn(),
    sendMessage: vi.fn(),
  },
}))

vi.mock('@/components/chat/MessageBubble', () => ({
  MessageBubble: ({ message }: { message: Message }) => <div>{message.content}</div>,
}))

vi.mock('@/components/chat/TypingIndicator', () => ({
  TypingIndicator: () => null,
}))

vi.mock('@/components/chat/MessageInput', () => ({
  MessageInput: () => null,
}))

const activeConversation: Conversation = {
  id: 55,
  listingId: 88,
  listingTitle: 'Camera body',
  otherUserId: 9,
  otherUserName: 'Seller Jane',
  otherUserAvatar: null,
  otherUserOnline: true,
  otherUserLastSeen: 'Last seen 1m ago',
  lastMessage: 'Still available?',
  lastMessageAt: '2026-03-25T00:00:00Z',
  unreadCount: 0,
  createdAt: '2026-03-24T00:00:00Z',
}

describe('chat reconnect catch-up', () => {
  beforeEach(() => {
    websocketMock.reset()
    vi.clearAllMocks()
    useChatStore.setState({
      conversations: [activeConversation],
      activeConversation,
      messages: [
        {
          id: 700,
          conversationId: 55,
          senderId: 9,
          senderName: 'Seller Jane',
          content: 'Existing message',
          imageUrl: null,
          status: 'DELIVERED',
          createdAt: '2026-03-25T01:00:00Z',
          isOwnMessage: false,
        },
      ],
      typingUsers: new Map(),
      isLoading: false,
      error: null,
    })
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: {
        id: 1,
        email: 'buyer@example.com',
        displayName: 'Buyer',
        firstName: 'Buyer',
        lastName: 'User',
        avatarUrl: null,
        bio: null,
        city: null,
        region: null,
        country: null,
        createdAt: '2026-03-20T00:00:00Z',
        updatedAt: '2026-03-20T00:00:00Z',
      },
      isAuthenticated: true,
      hasHydrated: true,
      isBootstrapping: false,
    })
    vi.mocked(chatApi.getMessages)
      .mockResolvedValueOnce({
        content: [
          {
            id: 700,
            conversationId: 55,
            senderId: 9,
            senderName: 'Seller Jane',
            content: 'Existing message',
            imageUrl: null,
            status: 'DELIVERED',
            createdAt: '2026-03-25T01:00:00Z',
            isOwnMessage: false,
          },
        ],
        totalElements: 1,
      })
      .mockResolvedValueOnce({
        content: [
          {
            id: 701,
            conversationId: 55,
            senderId: 9,
            senderName: 'Seller Jane',
            content: 'Reconnect sync message',
            imageUrl: null,
            status: 'DELIVERED',
            createdAt: '2026-03-25T01:05:00Z',
            isOwnMessage: false,
          },
        ],
        totalElements: 1,
      })
  })

  test('requests only messages newer than the latest persisted marker and appends them', async () => {
    const view = render(<ChatView conversation={activeConversation} />)

    await waitFor(() => {
      expect(useChatStore.getState().messages[0]?.id).toBe(700)
    })

    websocketMock.setConnectionState('connected')
    view.rerender(<ChatView conversation={activeConversation} />)

    await waitFor(() => {
      expect(vi.mocked(chatApi.getMessages)).toHaveBeenCalledWith(55, 0, 50, 700)
    })

    await waitFor(() => {
      expect(useChatStore.getState().messages).toHaveLength(2)
      expect(useChatStore.getState().messages[0]?.id).toBe(700)
      expect(useChatStore.getState().messages[1]?.id).toBe(701)
      expect(vi.mocked(chatApi.getConversation)).not.toHaveBeenCalled()
      expect(vi.mocked(chatApi.getConversations)).not.toHaveBeenCalled()
    })
  })
})
