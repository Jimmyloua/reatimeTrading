import { beforeEach, describe, expect, test, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import MessagesPage from '@/pages/MessagesPage'
import { useChatStore } from '@/stores/chatStore'
import { chatApi } from '@/api/chatApi'

vi.mock('@/components/chat/ConversationList', () => ({
  ConversationList: ({
    onSelectConversation,
    activeConversationId,
  }: {
    onSelectConversation: (conversationId: number) => void
    activeConversationId: number | null
  }) => (
    <div>
      <button onClick={() => onSelectConversation(55)}>Open conversation 55</button>
      <button onClick={() => onSelectConversation(77)}>Open conversation 77</button>
      <div data-testid="active-conversation-id">{activeConversationId ?? 'none'}</div>
    </div>
  ),
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

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location-search">{location.search || '(empty)'}</div>
}

describe('Messages page routing scaffolds', () => {
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
          lastMessageAt: '2026-03-22T00:00:00Z',
          unreadCount: 1,
          createdAt: '2026-03-20T00:00:00Z',
        },
      ],
      activeConversation: null,
      messages: [],
      typingUsers: new Map(),
      isLoading: false,
      error: null,
    })
  })

  test('reads the conversation query param and opens the matching conversation on first render', async () => {
    render(
      <MemoryRouter initialEntries={['/messages?conversation=55']}>
        <LocationProbe />
        <MessagesPage />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /messages/i })).toBeInTheDocument()
    expect(await screen.findByTestId('chat-view')).toHaveTextContent('55:Seller Jane')
    expect(screen.getByTestId('active-conversation-id')).toHaveTextContent('55')

    await waitFor(() => {
      expect(useChatStore.getState().conversations[0]?.unreadCount).toBe(0)
    })
  })

  test('fetches and inserts the conversation from the API when the URL targets a missing thread', async () => {
    vi.mocked(chatApi.getConversation).mockResolvedValue({
      id: 77,
      listingId: 99,
      listingTitle: 'Lens kit',
      otherUserId: 11,
      otherUserName: 'Seller Max',
      otherUserAvatar: null,
      lastMessage: 'Can ship today',
      lastMessageAt: '2026-03-22T01:00:00Z',
      unreadCount: 2,
      createdAt: '2026-03-21T00:00:00Z',
    })

    render(
      <MemoryRouter initialEntries={['/messages?conversation=77']}>
        <MessagesPage />
      </MemoryRouter>
    )

    expect(await screen.findByTestId('chat-view')).toHaveTextContent('77:Seller Max')
    expect(chatApi.getConversation).toHaveBeenCalledWith(77)

    await waitFor(() => {
      expect(useChatStore.getState().conversations.some((conversation) => conversation.id === 77)).toBe(true)
    })
  })

  test('falls back for invalid query params and updates the URL when a conversation is selected', async () => {
    render(
      <MemoryRouter initialEntries={['/messages?conversation=invalid']}>
        <LocationProbe />
        <MessagesPage />
      </MemoryRouter>
    )

    expect(screen.getByText(/select a conversation/i)).toBeInTheDocument()
    expect(screen.getByTestId('location-search')).toHaveTextContent('?conversation=invalid')

    fireEvent.click(screen.getByRole('button', { name: /open conversation 55/i }))

    expect(await screen.findByTestId('chat-view')).toHaveTextContent('55:Seller Jane')
    expect(screen.getByTestId('location-search')).toHaveTextContent('?conversation=55')
  })
})
