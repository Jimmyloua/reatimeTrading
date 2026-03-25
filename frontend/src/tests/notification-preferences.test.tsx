import { beforeEach, describe, expect, test, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NotificationsPage from '@/pages/NotificationsPage'
import { NotificationDropdown } from '@/components/notifications/NotificationDropdown'
import { useNotificationStore } from '@/stores/notificationStore'
import { notificationApi } from '@/api/notificationApi'

vi.mock('@/hooks/useNotifications', () => ({
  useNotifications: vi.fn(),
}))

vi.mock('@/api/notificationApi', () => ({
  notificationApi: {
    getNotifications: vi.fn(),
    getPreferences: vi.fn(),
    updatePreferences: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}))

describe('Notification preference scaffolds', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useNotificationStore.setState({
      notifications: [],
      unreadCount: 0,
      isLoading: false,
      error: null,
    })

    vi.mocked(notificationApi.getNotifications).mockResolvedValue({
      content: [
        {
          id: 501,
          type: 'TRANSACTION_UPDATE',
          title: 'Transaction updated',
          content: 'Your transaction has moved to the next step',
          referenceId: 88,
          referenceType: 'transaction',
          read: false,
          readAt: null,
          createdAt: '2026-03-22T00:00:00Z',
        },
      ],
      totalElements: 1,
    })
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(1)
    vi.mocked(notificationApi.getPreferences).mockResolvedValue({
      newMessageEnabled: true,
      itemSoldEnabled: false,
      transactionUpdateEnabled: true,
    })
    vi.mocked(notificationApi.updatePreferences).mockResolvedValue({
      newMessageEnabled: false,
      itemSoldEnabled: false,
      transactionUpdateEnabled: true,
    })
  })

  test('hydrates notifications page from persisted notifications and preferences', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /notifications/i })).toBeInTheDocument()

    expect(await screen.findByText('Transaction updated')).toBeInTheDocument()
    expect(await screen.findByRole('checkbox', { name: /new messages/i })).toBeChecked()
    expect(await screen.findByRole('checkbox', { name: /item sold/i })).not.toBeChecked()
    expect(await screen.findByRole('checkbox', { name: /transaction updates/i })).toBeChecked()
  })

  test('persists quick notification setting mutations from the dropdown', async () => {
    render(
      <MemoryRouter>
        <NotificationDropdown />
      </MemoryRouter>
    )

    const toggle = await screen.findByRole('checkbox', { name: /new messages/i })
    fireEvent.click(toggle)

    await waitFor(() => {
      expect(notificationApi.updatePreferences).toHaveBeenCalledWith({
        newMessageEnabled: false,
        itemSoldEnabled: false,
        transactionUpdateEnabled: true,
      })
    })
  })

  test('filters notification items immediately when quick settings are toggled off', async () => {
    useNotificationStore.setState({
      notifications: [
        {
          id: 501,
          type: 'ITEM_SOLD',
          title: 'Camera sold',
          content: 'Your listing has sold',
          referenceId: 88,
          referenceType: 'listing',
          read: false,
          readAt: null,
          createdAt: '2026-03-22T00:00:00Z',
        },
      ],
      preferences: {
        newMessageEnabled: true,
        itemSoldEnabled: true,
        transactionUpdateEnabled: true,
      },
      preferencesLoaded: true,
      unreadCount: 1,
      isLoading: false,
      error: null,
    })

    render(
      <MemoryRouter>
        <NotificationDropdown />
      </MemoryRouter>
    )

    expect(await screen.findByText('Camera sold')).toBeInTheDocument()

    fireEvent.click(await screen.findByRole('checkbox', { name: /item sold/i }))

    await waitFor(() => {
      expect(screen.queryByText('Camera sold')).not.toBeInTheDocument()
    })
  })
})
