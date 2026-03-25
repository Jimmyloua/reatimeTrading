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
    vi.mocked(chatApi.sendMessage).mockResolvedValue({
      id: 901,
      conversationId: 55,
      senderId: 1,
      senderName: 'Buyer',
      content: 'Offline send',
      imageUrl: null,
      status: 'SENT',
      createdAt: '2026-03-25T01:00:00Z',
      isOwnMessage: true,
    })
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

  test('reconnect rehydrate refreshes the active thread and non-active conversation previews and unread counts', async () => {
    websocketMock.setConnectionState('reconnecting')
    vi.mocked(chatApi.getConversation).mockResolvedValue({
      ...activeConversation,
      lastMessage: 'Authoritative preview',
      lastMessageAt: '2026-03-25T01:05:00Z',
    })
    vi.mocked(chatApi.getMessages).mockResolvedValue({
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
    vi.mocked(chatApi.getConversations).mockResolvedValue({
      content: [
        {
          ...activeConversation,
          lastMessage: 'Authoritative preview',
          lastMessageAt: '2026-03-25T01:05:00Z',
          unreadCount: 0,
        },
        {
          ...secondaryConversation,
          lastMessage: 'Background conversation refreshed',
          lastMessageAt: '2026-03-25T01:03:00Z',
          unreadCount: 4,
        },
      ],
      totalElements: 2,
    })

    const view = renderChatView()

    websocketMock.setConnectionState('connected')
    view.rerender(<ChatView conversation={activeConversation} />)

    await waitFor(() => {
      expect(chatApi.getConversation).toHaveBeenCalledWith(55)
      expect(chatApi.getMessages).toHaveBeenCalledWith(55)
      expect(chatApi.getConversations).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(useChatStore.getState().messages[0]?.content).toBe('Reconnect sync message')
      expect(useChatStore.getState().conversations.find((conversation) => conversation.id === 77)?.lastMessage).toBe(
        'Background conversation refreshed',
      )
      expect(useChatStore.getState().conversations.find((conversation) => conversation.id === 77)?.unreadCount).toBe(4)
    })
  })

  test('REST fallback sends reconcile non-active conversation preview and unread metadata immediately', async () => {
    websocketMock.setConnectionState('disconnected')
    vi.mocked(chatApi.getConversation).mockResolvedValue({
      ...activeConversation,
      lastMessage: 'Offline send',
      lastMessageAt: '2026-03-25T01:00:00Z',
    })
    vi.mocked(chatApi.getConversations).mockResolvedValue({
      content: [
        {
          ...activeConversation,
          lastMessage: 'Offline send',
          lastMessageAt: '2026-03-25T01:00:00Z',
          unreadCount: 0,
        },
        {
          ...secondaryConversation,
          lastMessage: 'Seller replied elsewhere',
          lastMessageAt: '2026-03-25T01:02:00Z',
          unreadCount: 3,
        },
      ],
      totalElements: 2,
    })

    renderChatView()

    act(() => {
      screen.getByRole('button', { name: 'Send message' }).click()
    })

    await waitFor(() => {
      expect(chatApi.sendMessage).toHaveBeenCalledWith(55, {
        content: 'Offline send',
        imageUrl: undefined,
      })
      expect(chatApi.getConversations).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(useChatStore.getState().conversations.find((conversation) => conversation.id === 77)?.lastMessage).toBe(
        'Seller replied elsewhere',
      )
      expect(useChatStore.getState().conversations.find((conversation) => conversation.id === 77)?.unreadCount).toBe(3)
    })
  })

  test('connected mode renders the sender message immediately and then rehydrates it from the server ack', async () => {
    vi.mocked(chatApi.getMessages)
      .mockResolvedValueOnce({
        content: [],
        totalElements: 0,
      })
      .mockResolvedValueOnce({
        content: [
          {
            id: 902,
            conversationId: 55,
            senderId: 1,
            senderName: 'Buyer',
            content: 'Offline send',
            imageUrl: null,
            status: 'DELIVERED',
            createdAt: '2026-03-25T01:00:01Z',
            isOwnMessage: true,
          },
        ],
        totalElements: 1,
      })

    renderChatView()

    act(() => {
      screen.getByRole('button', { name: 'Send message' }).click()
    })

    await waitFor(() => {
      expect(useChatStore.getState().messages[0]?.content).toBe('Offline send')
      expect(useChatStore.getState().messages[0]?.isOwnMessage).toBe(true)
    })

    act(() => {
      websocketMock.emit('/user/queue/message-ack', { messageId: 902, status: 'DELIVERED' })
    })

    await waitFor(() => {
      expect(chatApi.getMessages).toHaveBeenCalledTimes(2)
      expect(useChatStore.getState().messages[0]?.id).toBe(902)
      expect(useChatStore.getState().messages[0]?.status).toBe('DELIVERED')
    })
  })
})
