import { beforeEach, describe, expect, test, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { NotificationDropdown } from '@/components/notifications/NotificationDropdown'
import { useNotificationStore } from '@/stores/notificationStore'
import { notificationApi } from '@/api/notificationApi'

const mockedNavigate = vi.fn()

vi.mock('@/hooks/useNotifications', () => ({
  useNotifications: vi.fn(),
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  }
})

vi.mock('@/api/notificationApi', () => ({
  notificationApi: {
    markAsRead: vi.fn(),
    getPreferences: vi.fn(),
  },
}))

describe('Notification action scaffolds', () => {
  beforeEach(() => {
    vi.clearAllMocks()

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

  test('routes NEW_MESSAGE notifications into the matching /messages conversation context and marks them read', async () => {
    vi.mocked(notificationApi.markAsRead).mockResolvedValue(undefined)

    render(
      <MemoryRouter>
        <NotificationDropdown />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('New message'))

    await waitFor(() => {
      expect(notificationApi.markAsRead).toHaveBeenCalledWith(101)
      expect(mockedNavigate).toHaveBeenCalledWith('/messages?conversation=55')
    })
  })

  test.each([
    ['ITEM_SOLD', 'listing', 99, '/listings/99'],
    ['TRANSACTION_UPDATE', 'transaction', 77, '/transactions/77'],
  ] as const)(
    'routes %s notifications to %s details',
    async (type, referenceType, referenceId, expectedPath) => {
      vi.mocked(notificationApi.markAsRead).mockResolvedValue(undefined)

      useNotificationStore.setState({
        notifications: [
          {
            id: referenceId,
            type,
            title: `${type} title`,
            content: `${type} content`,
            referenceId,
            referenceType,
            read: false,
            readAt: null,
            createdAt: '2026-03-22T00:00:00Z',
          },
        ],
        unreadCount: 1,
        isLoading: false,
        error: null,
      })

      render(
        <MemoryRouter>
          <NotificationDropdown />
        </MemoryRouter>
      )

      fireEvent.click(screen.getByText(`${type} title`))

      await waitFor(() => {
        expect(notificationApi.markAsRead).toHaveBeenCalledWith(referenceId)
        expect(mockedNavigate).toHaveBeenCalledWith(expectedPath)
      })
    }
  )
})
