import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import UserProfilePage from '@/pages/UserProfilePage'
import { userApi } from '@/api/userApi'
import { ratingApi } from '@/api/ratingApi'
import { vi } from 'vitest'

vi.mock('@/api/userApi', () => ({
  userApi: {
    getUserById: vi.fn(),
  },
}))

vi.mock('@/api/ratingApi', () => ({
  ratingApi: {
    getRatingSummary: vi.fn(),
    getRecentRatings: vi.fn(),
  },
}))

const mockedUserApi = vi.mocked(userApi)
const mockedRatingApi = vi.mocked(ratingApi)

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/users/42']}>
        <Routes>
          <Route path="/users/:id" element={<UserProfilePage />} />
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('UserProfilePage', () => {
  beforeEach(() => {
    mockedUserApi.getUserById.mockResolvedValue({
      id: 42,
      email: '',
      displayName: 'Camera Trader',
      avatarUrl: null,
      createdAt: '2026-03-29T08:00:00Z',
      listingCount: 3,
    })
    mockedRatingApi.getRatingSummary.mockResolvedValue({
      userId: 42,
      averageRating: '4.8',
      totalRatings: 12,
      hasRatings: true,
    })
    mockedRatingApi.getRecentRatings.mockResolvedValue([
      {
        id: 1,
        transactionId: 77,
        raterId: 88,
        raterName: 'Buyer',
        ratedUserId: 42,
        rating: 5,
        reviewText: 'Fast and honest seller',
        visible: true,
        createdAt: '2026-03-28T10:00:00Z',
      },
    ])
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders the public profile for an anonymous viewer without redirecting to login', async () => {
    renderPage()

    await waitFor(() => {
      expect(mockedUserApi.getUserById).toHaveBeenCalledWith(42)
    })

    expect(
      await screen.findByRole('heading', { name: 'Camera Trader', level: 1 })
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Camera Trader', level: 2 })
    ).toBeInTheDocument()
    expect(await screen.findByText('@camera-trader')).toBeInTheDocument()
    expect(await screen.findByText('3 listings')).toBeInTheDocument()
    expect(await screen.findByText('Reviews')).toBeInTheDocument()
    expect(screen.queryByText('@undefined')).not.toBeInTheDocument()
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument()
  })
})
