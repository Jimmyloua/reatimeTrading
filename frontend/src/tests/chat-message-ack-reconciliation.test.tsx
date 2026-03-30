import { act, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { ChatView } from '@/components/chat/ChatView'
import { useAuthStore } from '@/stores/authStore'
import { useChatStore } from '@/stores/chatStore'
import { chatApi } from '@/api/chatApi'
import type { Conversation, Message } from '@/types/chat'

const websocketMock = vi.hoisted(() => {
  const subscribers = new Map<string, Array<(message: { body: string }) => void>>()
  const publish = vi.fn()

  return {
    subscribe: vi.fn((destination: string, callback: (message: { body: string }) => void) => {
      const current = subscribers.get(destination) ?? []
      current.push(callback)
      subscribers.set(destination, current)
      return {
        unsubscribe: () => {
          subscribers.set(destination, (subscribers.get(destination) ?? []).filter((entry) => entry !== callback))
        },
      }
    }),
    emit(destination: string, payload: unknown) {
      for (const callback of subscribers.get(destination) ?? []) {
        callback({ body: JSON.stringify(payload) })
      }
    },
    publish,
    reset() {
      subscribers.clear()
      publish.mockClear()
      this.subscribe.mockClear()
    },
  }
})

vi.mock('@/hooks/useWebSocket', () => ({
  useWebSocket: () => ({
    subscribe: websocketMock.subscribe,
    publish: websocketMock.publish,
    connectionState: 'connected' as const,
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
  MessageInput: ({ onSend }: { onSend: (content: string) => void | Promise<void> }) => (
    <button type="button" onClick={() => void onSend('Ack me')}>
      Send
    </button>
  ),
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

describe('chat ack reconciliation', () => {
  beforeEach(() => {
    websocketMock.reset()
    vi.clearAllMocks()
    useChatStore.setState({
      conversations: [activeConversation],
      activeConversation,
      messages: [],
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
    vi.mocked(chatApi.getMessages).mockResolvedValue({ content: [], totalElements: 0 })
  })

  test('replaces the optimistic row in place without fetching the conversation again', async () => {
    render(<ChatView conversation={activeConversation} />)

    act(() => {
      screen.getByRole('button', { name: 'Send' }).click()
    })

    const optimisticMessage = useChatStore.getState().messages[0]
    expect(optimisticMessage.id).toBeLessThan(0)

    act(() => {
      websocketMock.emit('/user/queue/message-ack', {
        clientMessageId: optimisticMessage.clientMessageId,
        messageId: 902,
        conversationId: 55,
        status: 'PERSISTED',
        createdAt: '2026-03-25T01:00:01Z',
      })
    })

    await waitFor(() => {
      expect(useChatStore.getState().messages).toHaveLength(1)
      expect(useChatStore.getState().messages[0]?.id).toBe(902)
      expect(useChatStore.getState().messages[0]?.status).toBe('PERSISTED')
      expect(vi.mocked(chatApi.getMessages)).toHaveBeenCalledTimes(1)
    })
  })
})
