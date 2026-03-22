import { beforeEach, describe, expect, test, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { NotificationDropdown } from '@/components/notifications/NotificationDropdown'
import { useNotificationStore } from '@/stores/notificationStore'

vi.mock('@/hooks/useNotifications', () => ({
  useNotifications: vi.fn(),
}))

vi.mock('@/api/notificationApi', () => ({
  notificationApi: {
    markAsRead: vi.fn(),
  },
}))

describe('Notification action scaffolds', () => {
  beforeEach(() => {
    useNotificationStore.setState({
      notifications: [
        {
          id: 101,
          type: 'NEW_MESSAGE',
          title: 'New message',
          content: 'Seller replied to your listing question',
          referenceId: 55,
          referenceType: 'conversation',
          read: false,
          readAt: null,
          createdAt: '2026-03-22T00:00:00Z',
        },
      ],
      unreadCount: 1,
      isLoading: false,
      error: null,
    })
  })

  test('renders dropdown shell with recent notification entry', () => {
    render(
      <MemoryRouter>
        <NotificationDropdown />
      </MemoryRouter>
    )

    expect(screen.getByText('Notifications')).toBeInTheDocument()
    expect(screen.getByText('1 unread')).toBeInTheDocument()
    expect(screen.getByText('New message')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /view all notifications/i })).toHaveAttribute(
      'href',
      '/notifications'
    )
  })

  test.todo('05-02 routes NEW_MESSAGE notifications into the matching /messages conversation context')
  test.todo('05-02 routes ITEM_SOLD and TRANSACTION_UPDATE notifications into their detail destinations')
})
