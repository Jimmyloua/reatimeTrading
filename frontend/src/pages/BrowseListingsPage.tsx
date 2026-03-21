import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { listingApi } from '@/api/listingApi'
import { ListingGrid } from '@/components/ListingGrid'
import { ListingFilters } from '@/components/ListingFilters'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { ListingSearchRequest, Condition } from '@/types/listing'

const DEFAULT_PAGE_SIZE = 20

export default function BrowseListingsPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  // Parse URL params
  const query = searchParams.get('query') || undefined
  const categoryId = searchParams.get('categoryId')
    ? parseInt(searchParams.get('categoryId')!, 10)
    : undefined
  const minPrice = searchParams.get('minPrice')
    ? parseFloat(searchParams.get('minPrice')!)
    : undefined
  const maxPrice = searchParams.get('maxPrice')
    ? parseFloat(searchParams.get('maxPrice')!)
    : undefined
  const conditions = searchParams.get('conditions')
    ? (searchParams.get('conditions')!.split(',') as Condition[])
    : undefined
  const city = searchParams.get('city') || undefined
  const region = searchParams.get('region') || undefined
  const page = searchParams.get('page') ? parseInt(searchParams.get('page')!, 10) : 0
  const sort = searchParams.get('sort') || 'createdAt,desc'

  // Fetch categories
  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: listingApi.getCategories,
  })

  // Fetch listings
  const searchRequest: ListingSearchRequest = {
    query,
    categoryId,
    minPrice,
    maxPrice,
    conditions,
    city,
    region,
    page,
    size: DEFAULT_PAGE_SIZE,
    sort,
  }

  const {
    data: listingsData,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['listings', searchRequest],
    queryFn: () => listingApi.searchListings(searchRequest),
  })

  // Update URL params
  const updateFilters = (newFilters: Partial<ListingSearchRequest>) => {
    const params = new URLSearchParams(searchParams)

    if (newFilters.query) params.set('query', newFilters.query)
    else params.delete('query')

    if (newFilters.categoryId) params.set('categoryId', String(newFilters.categoryId))
    else params.delete('categoryId')

    if (newFilters.minPrice !== undefined) params.set('minPrice', String(newFilters.minPrice))
    else params.delete('minPrice')

    if (newFilters.maxPrice !== undefined) params.set('maxPrice', String(newFilters.maxPrice))
    else params.delete('maxPrice')

    if (newFilters.conditions?.length) params.set('conditions', newFilters.conditions.join(','))
    else params.delete('conditions')

    if (newFilters.city) params.set('city', newFilters.city)
    else params.delete('city')

    if (newFilters.region) params.set('region', newFilters.region)
    else params.delete('region')

    if (newFilters.page) params.set('page', String(newFilters.page))
    else params.delete('page')

    if (newFilters.sort) params.set('sort', newFilters.sort)
    else params.delete('sort')

    setSearchParams(params)
  }

  const handleFilterChange = (filters: ListingSearchRequest) => {
    updateFilters({ ...filters, page: 0 }) // Reset to first page on filter change
  }

  const handlePageChange = (newPage: number) => {
    updateFilters({ page: newPage })
  }

  const handleSortChange = (newSort: string) => {
    updateFilters({ sort: newSort, page: 0 })
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="py-8 space-y-6">
        <Skeleton className="h-10 w-full" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="aspect-square rounded-lg" />
          ))}
        </div>
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="py-8">
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-destructive">Failed to load listings</p>
            <Button
              variant="outline"
              className="mt-4"
              onClick={() => window.location.reload()}
            >
              Retry
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  const listings = listingsData?.content || []
  const totalPages = listingsData?.totalPages || 0
  const totalElements = listingsData?.totalElements || 0
  const currentPage = listingsData?.number || 0

  return (
    <div className="py-8 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-foreground">Browse Listings</h1>
        {totalElements > 0 && (
          <p className="text-sm text-muted-foreground">
            {totalElements} {totalElements === 1 ? 'result' : 'results'}
          </p>
        )}
      </div>

      {/* Filters */}
      <ListingFilters
        filters={searchRequest}
        onFilterChange={handleFilterChange}
        categories={categories}
        onSortChange={handleSortChange}
        currentSort={sort}
      />

      {/* Listings Grid */}
      {listings.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">No listings found</p>
            <p className="text-sm text-muted-foreground mt-1">
              Try adjusting your filters or search terms
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <ListingGrid listings={listings} />

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
              >
                <ChevronLeft className="h-4 w-4" />
                Previous
              </Button>

              <div className="flex items-center gap-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let pageNum: number
                  if (totalPages <= 5) {
                    pageNum = i
                  } else if (currentPage < 3) {
                    pageNum = i
                  } else if (currentPage > totalPages - 3) {
                    pageNum = totalPages - 5 + i
                  } else {
                    pageNum = currentPage - 2 + i
                  }

                  return (
                    <Button
                      key={pageNum}
                      variant={pageNum === currentPage ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handlePageChange(pageNum)}
                    >
                      {pageNum + 1}
                    </Button>
                  )
                })}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
              >
                Next
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}