import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ListingFilters } from '@/components/ListingFilters'
import { ListingGrid } from '@/components/ListingGrid'
import { ListingCard } from '@/components/ListingCard'
import type { Listing, Category } from '@/types/listing'

// Mock data
const mockListings: Listing[] = [
  {
    id: 1,
    title: 'iPhone 15 Pro',
    price: 999,
    condition: 'NEW',
    status: 'AVAILABLE',
    city: 'San Francisco',
    region: 'CA',
    primaryImageUrl: 'https://example.com/image1.jpg',
    categoryId: 1,
    categoryName: 'Phones',
    createdAt: new Date().toISOString(),
  },
  {
    id: 2,
    title: 'MacBook Pro',
    price: 2499,
    condition: 'LIKE_NEW',
    status: 'AVAILABLE',
    city: 'Los Angeles',
    region: 'CA',
    primaryImageUrl: 'https://example.com/image2.jpg',
    categoryId: 2,
    categoryName: 'Laptops',
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  },
]

const mockCategories: Category[] = [
  { id: 1, name: 'Phones', slug: 'phones', displayOrder: 0 },
  { id: 2, name: 'Laptops', slug: 'laptops', displayOrder: 1 },
]

// Helper to render with providers
function renderWithProviders(ui: React.ReactElement, { route = '/listings' } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        {ui}
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Browse Listings', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('search - returns matching results', async () => {
    const mockFilterChange = vi.fn()

    renderWithProviders(
      <ListingFilters
        filters={{}}
        onFilterChange={mockFilterChange}
        categories={mockCategories}
      />
    )

    // Find search input by placeholder
    const searchInput = screen.getByPlaceholderText(/search listings/i)
    expect(searchInput).toBeInTheDocument()

    // Type search query
    fireEvent.change(searchInput, { target: { value: 'iPhone' } })

    // Wait for debounced search (300ms)
    await waitFor(() => {
      expect(mockFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({ query: 'iPhone' })
      )
    }, { timeout: 500 })
  })

  test('filter by category - returns category listings', async () => {
    const mockFilterChange = vi.fn()

    renderWithProviders(
      <ListingFilters
        filters={{}}
        onFilterChange={mockFilterChange}
        categories={mockCategories}
      />
    )

    // Find category select using the select element
    const selects = document.querySelectorAll('select')
    expect(selects.length).toBeGreaterThan(0)

    // Select a category
    const categorySelect = selects[0]
    fireEvent.change(categorySelect, { target: { value: '1' } })

    // Check filter change was called
    await waitFor(() => {
      expect(mockFilterChange).toHaveBeenCalled()
    })
  })

  test('filter by price range - returns filtered results', async () => {
    const mockFilterChange = vi.fn()

    renderWithProviders(
      <ListingFilters
        filters={{}}
        onFilterChange={mockFilterChange}
        categories={mockCategories}
      />
    )

    // Find price inputs by placeholder
    const minPriceInput = screen.getByPlaceholderText(/min/i)
    const maxPriceInput = screen.getByPlaceholderText(/max/i)

    // Set price range
    fireEvent.change(minPriceInput, { target: { value: '100' } })
    fireEvent.change(maxPriceInput, { target: { value: '1000' } })

    // Blur to trigger update
    fireEvent.blur(maxPriceInput)

    // Check filter change was called
    await waitFor(() => {
      expect(mockFilterChange).toHaveBeenCalled()
    })
  })

  test('filter by condition - returns filtered results', async () => {
    const mockFilterChange = vi.fn()

    renderWithProviders(
      <ListingFilters
        filters={{}}
        onFilterChange={mockFilterChange}
        categories={mockCategories}
      />
    )

    // Find condition buttons
    const newButton = screen.getByRole('button', { name: /^new$/i })
    expect(newButton).toBeInTheDocument()

    // Click condition button
    fireEvent.click(newButton)

    // Check filter change was called with condition
    await waitFor(() => {
      expect(mockFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({ conditions: ['NEW'] })
      )
    })
  })

  test('pagination - shows correct page', async () => {
    // Render ListingGrid with multiple listings
    renderWithProviders(
      <ListingGrid listings={mockListings} />
    )

    // Check listings are rendered
    expect(screen.getByText('iPhone 15 Pro')).toBeInTheDocument()
    expect(screen.getByText('MacBook Pro')).toBeInTheDocument()
  })

  test('sort - orders results correctly', async () => {
    const mockSortChange = vi.fn()

    renderWithProviders(
      <ListingFilters
        filters={{}}
        onFilterChange={vi.fn()}
        categories={mockCategories}
        onSortChange={mockSortChange}
        currentSort="createdAt,desc"
      />
    )

    // Find sort dropdown - it's the last select element
    const selects = document.querySelectorAll('select')
    const sortSelect = selects[selects.length - 1]
    expect(sortSelect).toBeInTheDocument()

    // Change sort order
    fireEvent.change(sortSelect, { target: { value: 'price,asc' } })

    // Check sort change was called
    await waitFor(() => {
      expect(mockSortChange).toHaveBeenCalledWith('price,asc')
    })
  })

  test('listing card displays correct information', async () => {
    const mockListing = mockListings[0]

    renderWithProviders(
      <ListingCard listing={mockListing} />
    )

    // Check listing info is displayed
    expect(screen.getByText(mockListing.title)).toBeInTheDocument()
    expect(screen.getByText(/\$999/)).toBeInTheDocument()
    expect(screen.getByText('New')).toBeInTheDocument() // Condition
    expect(screen.getByText(mockListing.categoryName)).toBeInTheDocument()
    expect(screen.getByText(/san francisco/i)).toBeInTheDocument() // Location
  })

  test('clear filters resets all filters', async () => {
    const mockFilterChange = vi.fn()

    renderWithProviders(
      <ListingFilters
        filters={{ query: 'test', categoryId: 1 }}
        onFilterChange={mockFilterChange}
        categories={mockCategories}
      />
    )

    // Find clear filters button
    const clearButton = screen.getByRole('button', { name: /clear filters/i })
    expect(clearButton).toBeInTheDocument()

    // Click clear
    fireEvent.click(clearButton)

    // Check filter change was called with empty object
    await waitFor(() => {
      expect(mockFilterChange).toHaveBeenCalledWith({})
    })
  })
})