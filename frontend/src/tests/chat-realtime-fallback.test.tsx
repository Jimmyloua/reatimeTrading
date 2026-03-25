import { act, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { ChatView } from '@/components/chat/ChatView'
import { useChatStore } from '@/stores/chatStore'
import { chatApi } from '@/api/chatApi'
import type { Conversation, Message } from '@/types/chat'

const useChatMock = vi.hoisted(() => ({
  connectionState: 'connected' as 'connecting' | 'connected' | 'disconnected' | 'reconnecting',
  sendMessage: vi.fn(),
  emitTyping: vi.fn(),
}))

vi.mock('@/hooks/useChat', () => ({
  useChat: () => useChatMock,
}))

vi.mock('@/hooks/useConversationPresence', () => ({
  useConversationPresence: () => ({
    isOnline: true,
    lastSeenText: 'Last seen 1m ago',
  }),
}))

vi.mock('@/api/chatApi', () => ({
  chatApi: {
    getConversation: vi.fn(),
    getMessages: vi.fn(),
  },
}))

vi.mock('@/components/chat/MessageBubble', () => ({
  MessageBubble: ({ message }: { message: Message }) => <div>{message.content}</div>,
}))

vi.mock('@/components/chat/TypingIndicator', () => ({
  TypingIndicator: () => <div data-testid="typing-indicator" />,
}))

vi.mock('@/components/chat/MessageInput', () => ({
  MessageInput: ({ statusMessage }: { statusMessage?: string | null }) => (
    <div>
      {statusMessage ? <p>{statusMessage}</p> : null}
      <button type="button">Send message</button>
    </div>
  ),
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
  },
}))

const conversation: Conversation = {
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

describe('chat realtime fallback contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useChatMock.connectionState = 'connected'

    useChatStore.setState({
      conversations: [],
      activeConversation: conversation,
      messages: [],
      typingUsers: new Map(),
      isLoading: false,
      error: null,
    })

    vi.mocked(chatApi.getConversation).mockResolvedValue(conversation)
    vi.mocked(chatApi.getMessages).mockResolvedValue({
      content: [],
    })
  })

  test('duplicate message ids are ignored instead of rendering the same message twice', () => {
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

    const store = useChatStore.getState()
    store.addMessage(duplicateMessage)
    store.addMessage(duplicateMessage)

    expect(useChatStore.getState().messages).toHaveLength(1)
  })

  test('conversation previews only reorder when lastMessageAt changes', () => {
    useChatStore.setState({
      conversations: [
        {
          ...conversation,
          id: 10,
          lastMessage: 'Newest row',
          lastMessageAt: '2026-03-25T00:10:00Z',
        },
        {
          ...conversation,
          id: 20,
          otherUserId: 12,
          otherUserName: 'Seller Max',
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

  test('degraded mode shows the offline helper copy above the composer', async () => {
    useChatMock.connectionState = 'disconnected'

    render(<ChatView conversation={conversation} />)

    expect(
      screen.getByText(
        'Live chat is offline right now, but messages will still send and the conversation will keep refreshing.',
      ),
    ).toBeInTheDocument()
  })

  test('connected mode avoids periodic fallback refreshes after the initial load', async () => {
    vi.useFakeTimers()
    render(<ChatView conversation={conversation} />)

    await act(async () => {
      await Promise.resolve()
    })

    expect(chatApi.getMessages).toHaveBeenCalledTimes(1)

    vi.mocked(chatApi.getConversation).mockClear()
    vi.mocked(chatApi.getMessages).mockClear()

    act(() => {
      vi.advanceTimersByTime(10_000)
    })

    await act(async () => {
      await Promise.resolve()
    })

    expect(chatApi.getConversation).not.toHaveBeenCalled()
    expect(chatApi.getMessages).not.toHaveBeenCalled()

    vi.useRealTimers()
  })
})
