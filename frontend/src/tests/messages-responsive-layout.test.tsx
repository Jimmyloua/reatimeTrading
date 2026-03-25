import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import MessagesPage from '@/pages/MessagesPage'
import { useChatStore } from '@/stores/chatStore'

vi.mock('@/components/chat/ConversationList', () => ({
  ConversationList: () => <div data-testid="conversation-list">Conversation list</div>,
}))

vi.mock('@/components/chat/ChatView', () => ({
  ChatView: ({ conversation }: { conversation: { id: number; otherUserName: string } }) => (
    <div data-testid="chat-view">
      {conversation.id}:{conversation.otherUserName}
    </div>
  ),
}))

vi.mock('@/api/chatApi', () => ({
  chatApi: {
    getConversation: vi.fn(),
  },
}))

function setViewport(mode: 'desktop' | 'tablet' | 'mobile') {
  const widths = {
    desktop: 1280,
    tablet: 900,
    mobile: 390,
  }

  Object.defineProperty(window, 'innerWidth', {
    configurable: true,
    writable: true,
    value: widths[mode],
  })

  window.dispatchEvent(new Event('resize'))
}

describe('messages responsive layout contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    useChatStore.setState({
      conversations: [
        {
          id: 55,
          listingId: 88,
          listingTitle: 'Camera body',
          otherUserId: 9,
          otherUserName: 'Seller Jane',
          otherUserAvatar: null,
          lastMessage: 'Still available?',
          lastMessageAt: '2026-03-25T00:00:00Z',
          unreadCount: 0,
          createdAt: '2026-03-24T00:00:00Z',
        },
      ],
      activeConversation: null,
      messages: [],
      typingUsers: new Map(),
      isLoading: false,
      error: null,
    })
  })

  test('desktop keeps the Active chats list and thread visible together', async () => {
    setViewport('desktop')

    render(
      <MemoryRouter initialEntries={['/messages?conversation=55']}>
        <MessagesPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Active chats')).toBeInTheDocument()
    expect(await screen.findByTestId('chat-view')).toBeInTheDocument()
  })

  test('tablet keeps the Active chats list and thread visible together', async () => {
    setViewport('tablet')

    render(
      <MemoryRouter initialEntries={['/messages?conversation=55']}>
        <MessagesPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Active chats')).toBeInTheDocument()
    expect(await screen.findByTestId('chat-view')).toBeInTheDocument()
  })

  test('mobile keeps only the conversation list visible before a thread is opened', () => {
    setViewport('mobile')

    render(
      <MemoryRouter initialEntries={['/messages']}>
        <MessagesPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Active chats')).toBeInTheDocument()
    expect(screen.queryByTestId('chat-view')).not.toBeInTheDocument()
    expect(screen.queryByText('Select a conversation')).not.toBeInTheDocument()
  })

  test('mobile thread mode shows a Back to conversations control and hides the list pane', async () => {
    setViewport('mobile')

    render(
      <MemoryRouter initialEntries={['/messages?conversation=55']}>
        <MessagesPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('chat-view')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Back to conversations' })).toBeInTheDocument()
    expect(screen.queryByText('Active chats')).not.toBeInTheDocument()
  })
})
