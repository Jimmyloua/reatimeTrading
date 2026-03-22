import { describe, expect, test, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NotificationsPage from '@/pages/NotificationsPage'

vi.mock('@/hooks/useNotifications', () => ({
  useNotifications: vi.fn(),
}))

vi.mock('@/components/notifications/NotificationList', () => ({
  NotificationList: () => <div data-testid="notification-list-stub">Notification list scaffold</div>,
}))

const phase5PreferenceProofPoints = [
  'quick-settings mutation persistence',
  'dropdown hydration from persisted preferences',
  'notifications page hydration from persisted preferences',
]

describe('Notification preference scaffolds', () => {
  test('renders notifications page shell for future preference coverage', () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /notifications/i })).toBeInTheDocument()
    expect(screen.getByTestId('notification-list-stub')).toBeInTheDocument()
    expect(phase5PreferenceProofPoints).toHaveLength(3)
  })

  test.todo('05-02 persists quick notification setting mutations through the backend preference API')
  test.todo('05-02 hydrates dropdown and notifications page state from persisted notification preferences')
})
