import { beforeEach, describe, expect, test, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import BrowseListingsPage from '@/pages/BrowseListingsPage'
import { contentApi } from '@/api/contentApi'
import { listingApi } from '@/api/listingApi'
import type { Category } from '@/types/listing'

vi.mock('@/api/listingApi', () => ({
  listingApi: {
    getCategories: vi.fn(),
    searchListings: vi.fn(),
  },
}))

vi.mock('@/api/contentApi', () => ({
  contentApi: {
    getCollection: vi.fn(),
  },
}))

const mockCategories: Category[] = [
  {
    id: 1,
    name: 'Cameras',
    slug: 'cameras',
    displayOrder: 0,
    children: [
      {
        id: 2,
        name: 'Mirrorless',
        slug: 'mirrorless',
        parentId: 1,
        displayOrder: 0,
      },
    ],
  },
]

function LocationProbe() {
  const location = useLocation()

  return <div data-testid="location-search">{location.search}</div>
}

function renderBrowsePage(route = '/listings?categoryId=9') {
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
        <BrowseListingsPage />
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Browse category disclosure contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(listingApi.getCategories).mockResolvedValue(mockCategories)
    vi.mocked(listingApi.searchListings).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0,
      first: true,
      last: true,
    })
    vi.mocked(contentApi.getCollection).mockResolvedValue({
      slug: 'staff-picks',
      title: 'Staff picks',
      subtitle: 'Editor selections',
      description: 'Featured devices',
      coverImageUrl: '/cover.jpg',
      targetType: 'collection',
      targetValue: 'staff-picks',
      displayOrder: 0,
      items: [],
    })
  })

  test('hover or focus preview does not commit categoryId until the category is explicitly selected', async () => {
    renderBrowsePage()

    const camerasTrigger = await screen.findByRole('button', { name: /cameras/i })

    fireEvent.mouseEnter(camerasTrigger)
    fireEvent.focus(camerasTrigger)

    expect(screen.getByTestId('location-search')).toHaveTextContent('?categoryId=9')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('collection=')

    fireEvent.click(await screen.findByRole('link', { name: /mirrorless/i }))

    await waitFor(() => {
      expect(screen.getByTestId('location-search')).toHaveTextContent('categoryId=2')
    })
  })

  test('keyboard selection commits categoryId on Enter while preview-only movement leaves the current URL unchanged', async () => {
    renderBrowsePage('/listings?categoryId=4&collection=staff-picks')

    const mirrorlessOption = await screen.findByRole('button', { name: /mirrorless/i })

    fireEvent.focus(mirrorlessOption)
    fireEvent.keyDown(mirrorlessOption, { key: 'ArrowDown' })

    expect(screen.getByTestId('location-search')).toHaveTextContent('categoryId=4')
    expect(screen.getByTestId('location-search')).toHaveTextContent('collection=staff-picks')

    fireEvent.keyDown(mirrorlessOption, { key: 'Enter', code: 'Enter' })

    await waitFor(() => {
      expect(screen.getByTestId('location-search')).toHaveTextContent('categoryId=2')
    })
  })
})
