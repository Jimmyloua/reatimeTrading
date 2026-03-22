import { describe, test, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from '@/App'
import { useAuthStore } from '@/stores/authStore'

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('App shell', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
    localStorage.clear()
  })

  test('renders logged-out home page', () => {
    renderApp()

    expect(
      screen.getByText(/buy and sell digital devices in a cleaner, faster marketplace/i)
    ).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /browse/i }).length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /create account/i })).toHaveLength(2)
  })

  test('renders authenticated navigation', () => {
    useAuthStore.setState({
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      isAuthenticated: true,
      user: {
        id: 1,
        email: 'test@example.com',
        displayName: 'Test User',
        avatarUrl: null,
        listingCount: 0,
        profileComplete: true,
        averageRating: null,
        totalRatings: 0,
        createdAt: new Date().toISOString(),
      },
      hasHydrated: true,
    })

    renderApp()

    expect(screen.getByText(/trading platform/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /transactions/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sell/i })).toBeInTheDocument()
  })
})
