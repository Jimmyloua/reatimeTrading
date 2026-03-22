import { beforeEach, describe, expect, test } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import MessagesPage from '@/pages/MessagesPage'
import { useChatStore } from '@/stores/chatStore'

describe('Messages page routing scaffolds', () => {
  beforeEach(() => {
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

  test('renders the current empty-state shell before deep-link bootstrap is implemented', () => {
    render(
      <MemoryRouter initialEntries={['/messages']}>
        <MessagesPage />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /messages/i })).toBeInTheDocument()
    expect(screen.getByText(/select a conversation/i)).toBeInTheDocument()
    expect(screen.getByText(/choose a conversation from the list to start chatting/i)).toBeInTheDocument()
  })

  test.todo('05-02 reads the conversation query param and opens the matching conversation on first render')
  test.todo('05-02 falls back safely when the conversation query param is missing or invalid')
})
