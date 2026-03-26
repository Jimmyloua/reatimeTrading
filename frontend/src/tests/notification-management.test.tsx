import { beforeEach, describe, expect, test, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import NotificationsPage from '@/pages/NotificationsPage'
import { notificationApi } from '@/api/notificationApi'
import { useNotificationStore } from '@/stores/notificationStore'

vi.mock('@/hooks/useNotifications', () => ({
  useNotifications: vi.fn(),
}))

vi.mock('@/api/notificationApi', () => ({
  notificationApi: {
    getNotifications: vi.fn(),
    getPreferences: vi.fn(),
    updatePreferences: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}))

function LocationProbe() {
  const location = useLocation()

  return <div data-testid="notification-location">{location.search}</div>
}

function renderNotificationsPage(route = '/notifications?tab=unread&types=NEW_MESSAGE,ITEM_SOLD&page=2') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <NotificationsPage />
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Notification management contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    useNotificationStore.setState({
      notifications: [
        {
          id: 101,
          type: 'NEW_MESSAGE',
          title: 'Unread message',
          content: 'Seller replied',
          referenceId: 55,
          referenceType: 'conversation',
          read: false,
          readAt: null,
          createdAt: '2026-03-26T00:00:00Z',
        },
        {
          id: 102,
          type: 'ITEM_SOLD',
          title: 'Sold item',
          content: 'Camera sold',
          referenceId: 99,
          referenceType: 'listing',
          read: false,
          readAt: null,
          createdAt: '2026-03-25T00:00:00Z',
        },
      ],
      preferences: {
        newMessageEnabled: true,
        itemSoldEnabled: true,
        transactionUpdateEnabled: true,
      },
      preferencesLoaded: false,
      unreadCount: 2,
      isLoading: false,
      error: null,
    })

    vi.mocked(notificationApi.getNotifications).mockResolvedValue({
      content: useNotificationStore.getState().notifications,
      totalElements: 2,
    })
    vi.mocked(notificationApi.getPreferences).mockResolvedValue({
      newMessageEnabled: true,
      itemSoldEnabled: true,
      transactionUpdateEnabled: false,
    })
    vi.mocked(notificationApi.updatePreferences).mockResolvedValue({
      newMessageEnabled: false,
      itemSoldEnabled: true,
      transactionUpdateEnabled: false,
    })
  })

  test('hydrates from URL-backed tab, types, and page filters and keeps the grouped preferences shell visible', async () => {
    renderNotificationsPage()

    expect(screen.getByTestId('notification-location')).toHaveTextContent(
      'tab=unread&types=NEW_MESSAGE,ITEM_SOLD&page=2'
    )
    expect(await screen.findByRole('tab', { name: /all/i })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: /unread/i })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('checkbox', { name: /new messages/i })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: /item sold/i })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: /transaction updates/i })).toBeInTheDocument()
    expect(screen.getByText(/conversation activity/i)).toBeInTheDocument()
    expect(screen.getByText(/selling activity/i)).toBeInTheDocument()
  })

  test('mark visible as read only affects the currently filtered rows', async () => {
    renderNotificationsPage('/notifications?tab=unread&types=NEW_MESSAGE&page=0')

    fireEvent.click(await screen.findByRole('button', { name: /mark visible as read/i }))

    await waitFor(() => {
      expect(notificationApi.markAllAsRead).toHaveBeenCalledWith({
        tab: 'unread',
        types: ['NEW_MESSAGE'],
        page: 0,
      })
    })

    expect(screen.getByText('Unread message')).toBeInTheDocument()
    expect(screen.queryByText('Sold item')).not.toBeInTheDocument()
  })
})
