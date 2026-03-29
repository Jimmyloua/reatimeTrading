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

    expect(await screen.findByAltText('Cameras')).toBeInTheDocument()
    expect(screen.queryByText('Mirrorless')).not.toBeInTheDocument()

    const camerasTrigger = await screen.findByRole('button', { name: /cameras/i })

    fireEvent.mouseEnter(camerasTrigger)
    fireEvent.focus(camerasTrigger)

    expect(screen.getByTestId('location-search')).toHaveTextContent('?categoryId=9')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('collection=')

    fireEvent.click(await screen.findByRole('button', { name: /browse mirrorless/i }))

    await waitFor(() => {
      expect(screen.getByTestId('location-search')).toHaveTextContent('categoryId=2')
    })
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('collection=')
  })

  test('preview-only hover leaves the URL untouched until the child action is explicitly selected', async () => {
    renderBrowsePage('/listings?categoryId=4&collection=staff-picks')

    const camerasTrigger = await screen.findByRole('button', { name: /cameras/i })

    fireEvent.mouseEnter(camerasTrigger)

    expect(screen.getByTestId('location-search')).toHaveTextContent('categoryId=4')
    expect(screen.getByTestId('location-search')).toHaveTextContent('collection=staff-picks')

    fireEvent.click(await screen.findByRole('button', { name: /browse mirrorless/i }))

    await waitFor(() => {
      expect(screen.getByTestId('location-search')).toHaveTextContent('categoryId=2')
    })
    expect(screen.getByTestId('location-search')).toHaveTextContent('collection=staff-picks')
  })

  test('moving the pointer away restores the image preview state', async () => {
    renderBrowsePage()

    const camerasTrigger = await screen.findByRole('button', { name: /cameras/i })
    fireEvent.mouseEnter(camerasTrigger)

    expect(await screen.findByRole('button', { name: /browse mirrorless/i })).toBeInTheDocument()

    fireEvent.mouseLeave(screen.getByTestId('browse-category-disclosure'))

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /browse mirrorless/i })).not.toBeInTheDocument()
    })
    expect(screen.getByAltText('Cameras')).toBeInTheDocument()
  })
})
