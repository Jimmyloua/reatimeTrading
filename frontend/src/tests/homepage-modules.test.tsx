import { beforeEach, describe, expect, test, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '@/App'
import { useAuthStore } from '@/stores/authStore'

vi.mock('@/hooks/useWebSocket', () => ({
  useWebSocket: () => ({
    client: null,
    connectionState: 'disconnected',
    connect: vi.fn(),
    disconnect: vi.fn(),
    subscribe: vi.fn(),
    publish: vi.fn(),
  }),
}))

const homepageModules = [
  {
    id: 'hero-1',
    type: 'hero',
    displayOrder: 0,
    title: 'Fresh arrivals',
    ctaLabel: 'Browse mirrorless cameras',
    destination: '/listings?categoryId=12',
  },
  {
    id: 'tiles-1',
    type: 'image_tiles',
    displayOrder: 1,
    title: 'Capture kits',
    ctaLabel: 'Shop featured cameras',
    destination: '/listings?collection=featured-cameras',
  },
  {
    id: 'row-1',
    type: 'collection_row',
    displayOrder: 2,
    title: 'Collector favorites',
    ctaLabel: 'Open staff picks',
    destination: '/listings?collection=staff-picks',
  },
  {
    id: 'spotlight-1',
    type: 'category_spotlight',
    displayOrder: 3,
    title: 'Audio spotlight',
    ctaLabel: 'Browse audio gear',
    destination: '/listings?categoryId=44',
  },
]

function renderAppHome() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Homepage module contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().logout()
    localStorage.clear()
    sessionStorage.clear()
  })

  test('renders homepage modules from server-driven content in displayOrder', async () => {
    renderAppHome()

    const headings = await screen.findAllByRole('heading', {
      name: /fresh arrivals|capture kits|collector favorites|audio spotlight/i,
    })

    expect(headings.map((heading) => heading.textContent)).toEqual(
      homepageModules.map((module) => module.title)
    )
  })

  test('homepage module CTAs navigate to exact browse URLs for categoryId and collection entry points', async () => {
    renderAppHome()

    expect(
      await screen.findByRole('link', { name: /browse mirrorless cameras/i })
    ).toHaveAttribute('href', '/listings?categoryId=12')
    expect(
      screen.getByRole('link', { name: /shop featured cameras/i })
    ).toHaveAttribute('href', '/listings?collection=featured-cameras')
    expect(
      screen.getByRole('link', { name: /open staff picks/i })
    ).toHaveAttribute('href', '/listings?collection=staff-picks')
    expect(
      screen.getByRole('link', { name: /browse audio gear/i })
    ).toHaveAttribute('href', '/listings?categoryId=44')
  })
})
