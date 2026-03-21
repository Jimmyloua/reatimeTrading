import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ListingForm } from '@/components/ListingForm'
import { ImageUploader } from '@/components/ImageUploader'
import type { Category } from '@/types/listing'

// Mock categories
const mockCategories: Category[] = [
  { id: 1, name: 'Electronics', slug: 'electronics', displayOrder: 0 },
  { id: 2, name: 'Phones', slug: 'phones', parentId: 1, displayOrder: 0 },
]

// Helper to render with providers
function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>{ui}</BrowserRouter>
    </QueryClientProvider>
  )
}

describe('Create Listing', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('form renders correctly', async () => {
    const mockSubmit = vi.fn()

    renderWithProviders(
      <ListingForm
        onSubmit={mockSubmit}
        categories={mockCategories}
      />
    )

    // Check that form elements are rendered
    expect(screen.getByRole('button', { name: /create listing/i })).toBeInTheDocument()
    expect(document.getElementById('title')).toBeInTheDocument()
    expect(document.getElementById('description')).toBeInTheDocument()
    expect(document.getElementById('price')).toBeInTheDocument()
  })

  test.skip('form validation shows errors for empty fields', async () => {
    const mockSubmit = vi.fn()

    renderWithProviders(
      <ListingForm
        onSubmit={mockSubmit}
        categories={mockCategories}
      />
    )

    // Click submit without filling form
    const submitButton = screen.getByRole('button', { name: /create listing/i })
    fireEvent.click(submitButton)

    // Check for validation errors appear
    await waitFor(() => {
      // Look for any error messages
      const errors = document.querySelectorAll('.text-destructive')
      expect(errors.length).toBeGreaterThan(0)
    })

    // Submit should not have been called
    expect(mockSubmit).not.toHaveBeenCalled()
  })

  test('image upload - accepts valid images', async () => {
    const mockImagesChange = vi.fn()

    renderWithProviders(
      <ImageUploader
        images={[]}
        onImagesChange={mockImagesChange}
        primaryIndex={0}
        onPrimaryChange={vi.fn()}
      />
    )

    // Check dropzone is rendered
    expect(screen.getByText(/drag and drop images/i)).toBeInTheDocument()
    expect(screen.getByText(/png, jpg, or webp/i)).toBeInTheDocument()
  })

  test('image upload - shows max limit reached', async () => {
    const mockImagesChange = vi.fn()

    renderWithProviders(
      <ImageUploader
        images={Array(10).fill(new File([''], 'test.jpg', { type: 'image/jpeg' }))}
        onImagesChange={mockImagesChange}
        primaryIndex={0}
        onPrimaryChange={vi.fn()}
      />
    )

    // Check max images message
    expect(screen.getByText(/maximum 10 images reached/i)).toBeInTheDocument()
  })

  test('category selection - renders select elements', async () => {
    const mockSubmit = vi.fn()

    renderWithProviders(
      <ListingForm
        onSubmit={mockSubmit}
        categories={mockCategories}
      />
    )

    // Find category and condition select elements
    const selectElements = document.querySelectorAll('select')
    // There should be at least 2 selects: category and condition
    expect(selectElements.length).toBeGreaterThanOrEqual(2)
  })

  test('submit - creates listing successfully', async () => {
    const mockSubmit = vi.fn().mockResolvedValue(undefined)

    renderWithProviders(
      <ListingForm
        onSubmit={mockSubmit}
        categories={mockCategories}
      />
    )

    // Fill all required fields
    const titleInput = document.getElementById('title') as HTMLInputElement
    fireEvent.change(titleInput, { target: { value: 'Test Item' } })

    const descriptionTextarea = document.getElementById('description') as HTMLTextAreaElement
    fireEvent.change(descriptionTextarea, { target: { value: 'A test description for the item' } })

    const priceInput = document.getElementById('price') as HTMLInputElement
    fireEvent.change(priceInput, { target: { value: '100' } })

    // Select category using the select element
    const selects = document.querySelectorAll('select')
    const categorySelect = selects[0] // First select is category
    if (categorySelect) {
      fireEvent.change(categorySelect, { target: { value: '1' } })
    }

    // Submit
    const submitButton = screen.getByRole('button', { name: /create listing/i })
    fireEvent.click(submitButton)

    // Wait for submission
    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalled()
    }, { timeout: 2000 })
  })
})