import { act, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { ChatView } from '@/components/chat/ChatView'
import { useChatStore } from '@/stores/chatStore'
import { useAuthStore } from '@/stores/authStore'
import { chatApi } from '@/api/chatApi'
import type { Conversation, Message } from '@/types/chat'

const websocketMock = vi.hoisted(() => {
  let connectionState: 'connecting' | 'connected' | 'disconnected' | 'reconnecting' = 'connected'
  const subscribers = new Map<string, Array<(message: { body: string }) => void>>()
  const publish = vi.fn()

  return {
    subscribe: vi.fn((destination: string, callback: (message: { body: string }) => void) => {
      const current = subscribers.get(destination) ?? []
      current.push(callback)
      subscribers.set(destination, current)

      return {
        unsubscribe: () => {
          subscribers.set(
            destination,
            (subscribers.get(destination) ?? []).filter((candidate) => candidate !== callback),
          )
        },
      }
    }),
    emit(destination: string, payload: unknown) {
      for (const callback of subscribers.get(destination) ?? []) {
        callback({ body: JSON.stringify(payload) })
      }
    },
    publish,
    getConnectionState() {
      return connectionState
    },
    setConnectionState(nextState: 'connecting' | 'connected' | 'disconnected' | 'reconnecting') {
      connectionState = nextState
    },
    reset() {
      connectionState = 'connected'
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
  TypingIndicator: () => <div data-testid="typing-indicator" />,
}))

vi.mock('@/components/chat/MessageInput', () => ({
  MessageInput: ({
    onSend,
    statusMessage,
  }: {
    onSend: (content: string, imageUrl?: string) => void | Promise<void>
    statusMessage?: string | null
  }) => (
    <div>
      {statusMessage ? <p>{statusMessage}</p> : null}
      <button type="button" onClick={() => void onSend('Offline send')}>
        Send message
      </button>
    </div>
  ),
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
  },
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

const secondaryConversation: Conversation = {
  id: 77,
  listingId: 99,
  listingTitle: 'Lens kit',
  otherUserId: 12,
  otherUserName: 'Seller Max',
  otherUserAvatar: null,
  otherUserOnline: false,
  otherUserLastSeen: 'Last seen 4m ago',
  lastMessage: 'Original preview',
  lastMessageAt: '2026-03-24T23:00:00Z',
  unreadCount: 1,
  createdAt: '2026-03-23T00:00:00Z',
}

function renderChatView() {
  return render(<ChatView conversation={activeConversation} />)
}

describe('chat realtime fallback contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    websocketMock.reset()

    useChatStore.setState({
      conversations: [activeConversation, secondaryConversation],
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

    vi.mocked(chatApi.getConversation).mockResolvedValue(activeConversation)
    vi.mocked(chatApi.getConversations).mockResolvedValue({
      content: [activeConversation, secondaryConversation],
      totalElements: 2,
    })
    vi.mocked(chatApi.getMessages).mockResolvedValue({
      content: [],
      totalElements: 0,
    })
    vi.mocked(chatApi.sendMessage).mockImplementation(async (_conversationId, request) => ({
      clientMessageId: request.clientMessageId ?? null,
      messageId: 901,
      conversationId: 55,
      status: 'PERSISTED',
      createdAt: '2026-03-25T01:00:00Z',
    }))
  })

  test('duplicate message ids are ignored instead of rendering the same message twice', async () => {
    renderChatView()

    const duplicateMessage: Message = {
      id: 101,
      conversationId: 55,
      senderId: 9,
      senderName: 'Seller Jane',
      content: 'Hello again',
      imageUrl: null,
      status: 'DELIVERED',
      createdAt: '2026-03-25T00:00:00Z',
      isOwnMessage: false,
    }

    act(() => {
      websocketMock.emit('/user/queue/messages', duplicateMessage)
      websocketMock.emit('/user/queue/messages', duplicateMessage)
    })

    await waitFor(() => {
      expect(useChatStore.getState().messages).toHaveLength(1)
    })
  })

  test('conversation previews only reorder when lastMessageAt changes', () => {
    useChatStore.setState({
      conversations: [
        {
          ...activeConversation,
          id: 10,
          lastMessage: 'Newest row',
          lastMessageAt: '2026-03-25T00:10:00Z',
        },
        {
          ...secondaryConversation,
          id: 20,
          lastMessage: 'Stable row',
          lastMessageAt: '2026-03-25T00:05:00Z',
        },
      ],
    })

    useChatStore.getState().syncConversationPreview({
      id: 301,
      conversationId: 20,
      senderId: 12,
      senderName: 'Seller Max',
      content: 'Duplicate preview payload',
      imageUrl: null,
      status: 'DELIVERED',
      createdAt: '2026-03-25T00:05:00Z',
      isOwnMessage: false,
    })

    expect(useChatStore.getState().conversations.map((current) => current.id)).toEqual([10, 20])
  })

  test('degraded mode shows the offline helper copy above the composer', () => {
    websocketMock.setConnectionState('disconnected')

    renderChatView()

    expect(
      screen.getByText(
        'Live chat is offline right now, but messages will still send and the conversation will keep refreshing.',
      ),
    ).toBeInTheDocument()
  })

  test('connected mode avoids periodic fallback refreshes after the initial load', async () => {
    vi.useFakeTimers()
    renderChatView()

    await act(async () => {
      await Promise.resolve()
    })

    expect(chatApi.getMessages).toHaveBeenCalledTimes(1)

    vi.mocked(chatApi.getConversation).mockClear()
    vi.mocked(chatApi.getMessages).mockClear()
    vi.mocked(chatApi.getConversations).mockClear()

    act(() => {
      vi.advanceTimersByTime(10_000)
    })

    await act(async () => {
      await Promise.resolve()
    })

    expect(chatApi.getConversation).not.toHaveBeenCalled()
    expect(chatApi.getMessages).not.toHaveBeenCalled()
    expect(chatApi.getConversations).not.toHaveBeenCalled()

    vi.useRealTimers()
  })

  test('reconnect catch-up appends only missing persisted messages after the latest cursor', async () => {
    websocketMock.setConnectionState('reconnecting')
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
    const view = renderChatView()

    await waitFor(() => {
      expect(useChatStore.getState().messages[0]?.id).toBe(700)
    })

    websocketMock.setConnectionState('connected')
    view.rerender(<ChatView conversation={activeConversation} />)

    await waitFor(() => {
      expect(chatApi.getMessages).toHaveBeenCalledWith(55, 0, 50, 700)
    })

    await waitFor(() => {
      expect(useChatStore.getState().messages).toHaveLength(2)
      expect(useChatStore.getState().messages[0]?.id).toBe(700)
      expect(useChatStore.getState().messages[1]?.id).toBe(701)
    })
  })

  test('REST fallback sends reconcile the optimistic message without a follow-up refresh', async () => {
    websocketMock.setConnectionState('disconnected')
    vi.mocked(chatApi.sendMessage).mockResolvedValue({
      clientMessageId: 'client-1',
      messageId: 901,
      conversationId: 55,
      status: 'PERSISTED',
      createdAt: '2026-03-25T01:00:00Z',
    })

    renderChatView()

    await waitFor(() => {
      expect(chatApi.getMessages).toHaveBeenCalled()
    })

    act(() => {
      screen.getByRole('button', { name: 'Send message' }).click()
    })

    await waitFor(() => {
      expect(chatApi.sendMessage).toHaveBeenCalledWith(55, {
        content: 'Offline send',
        imageUrl: undefined,
        clientMessageId: expect.any(String),
      })
    })

    expect(vi.mocked(chatApi.getConversations)).not.toHaveBeenCalled()
    expect(vi.mocked(chatApi.getConversation)).not.toHaveBeenCalled()
  })

  test('connected mode reconciles the sender message in place from the persisted ack', async () => {
    renderChatView()

    await waitFor(() => {
      expect(chatApi.getMessages).toHaveBeenCalled()
    })

    act(() => {
      screen.getByRole('button', { name: 'Send message' }).click()
    })

    const publishPayload = websocketMock.publish.mock.calls[0]?.[1]
    const parsedPayload = JSON.parse(publishPayload)

    act(() => {
      websocketMock.emit('/user/queue/message-ack', {
        clientMessageId: parsedPayload.clientMessageId,
        messageId: 902,
        conversationId: 55,
        status: 'PERSISTED',
        createdAt: '2026-03-25T01:00:01Z',
      })
    })

    await waitFor(() => {
      expect(parsedPayload.clientMessageId).toBeTruthy()
      expect(parsedPayload.conversationId).toBe(55)
      expect(chatApi.getMessages).toHaveBeenCalledTimes(1)
    })
  })
})
