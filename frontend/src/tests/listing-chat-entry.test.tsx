import { beforeEach, describe, expect, test, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import ListingDetailPage from '@/pages/ListingDetailPage'
import { useAuthStore } from '@/stores/authStore'
import { listingApi } from '@/api/listingApi'
import { chatApi } from '@/api/chatApi'

vi.mock('@/api/listingApi', () => ({
  listingApi: {
    getListingDetail: vi.fn(),
  },
}))

vi.mock('@/api/chatApi', () => ({
  chatApi: {
    createConversation: vi.fn(),
  },
}))

vi.mock('@/components/transaction/RequestToBuyButton', () => ({
  RequestToBuyButton: () => <button type="button">Request to buy</button>,
}))

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location-path">{`${location.pathname}${location.search}`}</div>
}

describe('Listing chat entry scaffolds', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      isAuthenticated: true,
      user: {
        id: 99,
        email: 'buyer@example.com',
        displayName: 'Buyer',
        avatarUrl: null,
        profileComplete: true,
        averageRating: null,
        totalRatings: 0,
        createdAt: '2026-03-01T00:00:00Z',
      },
    })

    vi.mocked(listingApi.getListingDetail).mockResolvedValue({
      id: 12,
      title: 'Mirrorless camera',
      description: 'Excellent condition body only',
      price: 700,
      condition: 'GOOD',
      status: 'AVAILABLE',
      city: 'Shanghai',
      region: 'Shanghai',
      categoryId: 5,
      categoryName: 'Cameras',
      createdAt: '2026-03-15T00:00:00Z',
      updatedAt: '2026-03-15T00:00:00Z',
      images: [
        {
          id: 1,
          imageUrl: 'https://example.com/camera.jpg',
          isPrimary: true,
          displayOrder: 0,
        },
      ],
      seller: {
        id: 7,
        displayName: 'Seller Jane',
        avatarUrl: undefined,
        memberSince: '2024-01-01T00:00:00Z',
        listingCount: 4,
      },
    })
    vi.mocked(chatApi.createConversation).mockResolvedValue({
      id: 401,
      listingId: 12,
      listingTitle: 'Mirrorless camera',
      otherUserId: 7,
      otherUserName: 'Seller Jane',
      otherUserAvatar: null,
      lastMessage: null,
      lastMessageAt: null,
      unreadCount: 0,
      createdAt: '2026-03-22T00:00:00Z',
    })
  })

  test('renders seller context on listing detail while Phase 5 chat CTA work is pending', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/listings/12']}>
          <Routes>
            <Route path="/listings/:id" element={<ListingDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    )

    await waitFor(() => {
      expect(screen.getAllByText('Mirrorless camera')).toHaveLength(2)
    })

    expect(screen.getByRole('link', { name: /seller jane/i })).toHaveAttribute('href', '/users/7')
    expect(screen.getByRole('button', { name: /request to buy/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /chat with seller/i })).toBeInTheDocument()
  })

  test('05-03 shows a seller chat CTA for non-owners and boots or resumes the correct conversation', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/listings/12']}>
          <Routes>
            <Route path="/listings/:id" element={<><LocationProbe /><ListingDetailPage /></>} />
            <Route path="/messages" element={<LocationProbe />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    )

    const chatButton = await screen.findByRole('button', { name: /chat with seller/i })
    fireEvent.click(chatButton)

    await waitFor(() => {
      expect(chatApi.createConversation).toHaveBeenCalledWith({ listingId: 12 })
      expect(screen.getByTestId('location-path')).toHaveTextContent('/messages?conversation=401')
    })
  })

  test('05-03 hides the seller chat CTA when the viewer owns the listing', async () => {
    useAuthStore.setState({
      accessToken: 'owner-access',
      refreshToken: 'owner-refresh',
      isAuthenticated: true,
      user: {
        id: 7,
        email: 'seller@example.com',
        displayName: 'Seller Jane',
        avatarUrl: null,
        profileComplete: true,
        averageRating: null,
        totalRatings: 0,
        createdAt: '2026-03-01T00:00:00Z',
      },
    })

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/listings/12']}>
          <Routes>
            <Route path="/listings/:id" element={<ListingDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    )

    await waitFor(() => {
      expect(screen.getAllByText('Mirrorless camera')).toHaveLength(2)
    })

    expect(screen.queryByRole('button', { name: /chat with seller/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /edit listing/i })).toBeInTheDocument()
  })
})
