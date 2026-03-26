import { beforeEach, describe, expect, test, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '@/App'
import { contentApi } from '@/api/contentApi'
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

vi.mock('@/api/contentApi', () => ({
  contentApi: {
    getHomepage: vi.fn(),
  },
}))

const homepageModules = [
  {
    slug: 'hero-1',
    moduleType: 'hero',
    displayOrder: 0,
    title: 'Fresh arrivals',
    subtitle: 'Editor-curated arrivals',
    items: [
      {
        imageUrl: '/hero.jpg',
        headline: 'Browse mirrorless cameras',
        subheadline: 'Start in cameras',
        linkType: 'category',
        linkValue: '12',
        accentLabel: 'Fresh arrivals',
        displayOrder: 0,
      },
    ],
  },
  {
    slug: 'tiles-1',
    moduleType: 'image_tiles',
    displayOrder: 1,
    title: 'Capture kits',
    subtitle: 'Tile picks',
    items: [
      {
        imageUrl: '/tiles.jpg',
        headline: 'Shop featured cameras',
        subheadline: 'Featured camera gear',
        linkType: 'collection',
        linkValue: 'featured-cameras',
        accentLabel: 'Capture kits',
        displayOrder: 0,
      },
    ],
  },
  {
    slug: 'row-1',
    moduleType: 'collection_row',
    displayOrder: 2,
    title: 'Collector favorites',
    subtitle: 'Rows of favorites',
    items: [
      {
        imageUrl: '/row.jpg',
        headline: 'Open staff picks',
        subheadline: 'Staff curated picks',
        linkType: 'collection',
        linkValue: 'staff-picks',
        accentLabel: 'Collector favorites',
        displayOrder: 0,
      },
    ],
  },
  {
    slug: 'spotlight-1',
    moduleType: 'category_spotlight',
    displayOrder: 3,
    title: 'Audio spotlight',
    subtitle: 'Audio category',
    items: [
      {
        imageUrl: '/spotlight.jpg',
        headline: 'Browse audio gear',
        subheadline: 'Headphones and speakers',
        linkType: 'category',
        linkValue: '44',
        accentLabel: 'Audio spotlight',
        displayOrder: 0,
      },
    ],
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
    vi.mocked(contentApi.getHomepage).mockResolvedValue({
      modules: homepageModules,
    })
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
