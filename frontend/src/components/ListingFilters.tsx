import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { CategorySelect } from '@/components/CategorySelect'
import { Search, X } from 'lucide-react'
import type { Category, ListingSearchRequest, Condition } from '@/types/listing'

interface ListingFiltersProps {
  filters: ListingSearchRequest
  onFilterChange: (filters: ListingSearchRequest) => void
  categories: Category[]
  onSortChange?: (sort: string) => void
  currentSort?: string
}

const CONDITION_OPTIONS: { value: Condition; label: string }[] = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
  { value: 'POOR', label: 'Poor' },
]

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest First' },
  { value: 'createdAt,asc', label: 'Oldest First' },
  { value: 'price,asc', label: 'Price: Low to High' },
  { value: 'price,desc', label: 'Price: High to Low' },
]

export function ListingFilters({
  filters,
  onFilterChange,
  categories,
  onSortChange,
  currentSort,
}: ListingFiltersProps) {
  const [searchQuery, setSearchQuery] = useState(filters.query || '')
  const [minPrice, setMinPrice] = useState(filters.minPrice?.toString() || '')
  const [maxPrice, setMaxPrice] = useState(filters.maxPrice?.toString() || '')
  const [selectedConditions, setSelectedConditions] = useState<Condition[]>(
    filters.conditions || []
  )
  const [city, setCity] = useState(filters.city || '')
  const [region, setRegion] = useState(filters.region || '')

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery !== filters.query) {
        onFilterChange({ ...filters, query: searchQuery || undefined })
      }
    }, 300)

    return () => clearTimeout(timer)
  }, [searchQuery])

  const handleCategoryChange = (categoryId: number | undefined) => {
    onFilterChange({ ...filters, categoryId })
  }

  const handleConditionToggle = (condition: Condition) => {
    const newConditions = selectedConditions.includes(condition)
      ? selectedConditions.filter((c) => c !== condition)
      : [...selectedConditions, condition]

    setSelectedConditions(newConditions)
    onFilterChange({
      ...filters,
      conditions: newConditions.length > 0 ? newConditions : undefined,
    })
  }

  const handlePriceChange = () => {
    onFilterChange({
      ...filters,
      minPrice: minPrice ? parseFloat(minPrice) : undefined,
      maxPrice: maxPrice ? parseFloat(maxPrice) : undefined,
    })
  }

  const handleLocationChange = () => {
    onFilterChange({
      ...filters,
      city: city || undefined,
      region: region || undefined,
    })
  }

  const handleClearFilters = () => {
    setSearchQuery('')
    setMinPrice('')
    setMaxPrice('')
    setSelectedConditions([])
    setCity('')
    setRegion('')
    onFilterChange({})
  }

  const hasActiveFilters =
    filters.query ||
    filters.categoryId ||
    filters.minPrice ||
    filters.maxPrice ||
    filters.conditions?.length ||
    filters.city ||
    filters.region

  return (
    <div className="space-y-4">
      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="text"
          placeholder="Search listings..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="pl-10"
        />
      </div>

      {/* Filters Row */}
      <div className="flex flex-wrap gap-4">
        {/* Category */}
        <div className="w-48">
          <CategorySelect
            value={filters.categoryId}
            onChange={handleCategoryChange}
            categories={categories}
            placeholder="All Categories"
          />
        </div>

        {/* Sort */}
        {onSortChange && (
          <select
            value={currentSort || 'createdAt,desc'}
            onChange={(e) => onSortChange(e.target.value)}
            className="flex h-8 rounded-lg border border-input bg-transparent px-2.5 py-1 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:text-sm"
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        )}
      </div>

      {/* Price Range */}
      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Min Price</label>
          <Input
            type="number"
            placeholder="Min"
            value={minPrice}
            onChange={(e) => setMinPrice(e.target.value)}
            onBlur={handlePriceChange}
            className="w-28"
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Max Price</label>
          <Input
            type="number"
            placeholder="Max"
            value={maxPrice}
            onChange={(e) => setMaxPrice(e.target.value)}
            onBlur={handlePriceChange}
            className="w-28"
          />
        </div>
      </div>

      {/* Condition */}
      <div className="space-y-2">
        <label className="text-sm font-medium">Condition</label>
        <div className="flex flex-wrap gap-2">
          {CONDITION_OPTIONS.map((option) => (
            <Button
              key={option.value}
              type="button"
              variant={selectedConditions.includes(option.value) ? 'default' : 'outline'}
              size="sm"
              onClick={() => handleConditionToggle(option.value)}
            >
              {option.label}
            </Button>
          ))}
        </div>
      </div>

      {/* Location */}
      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">City</label>
          <Input
            type="text"
            placeholder="City"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            onBlur={handleLocationChange}
            className="w-36"
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Region</label>
          <Input
            type="text"
            placeholder="State/Province"
            value={region}
            onChange={(e) => setRegion(e.target.value)}
            onBlur={handleLocationChange}
            className="w-36"
          />
        </div>
      </div>

      {/* Clear Filters */}
      {hasActiveFilters && (
        <Button variant="ghost" size="sm" onClick={handleClearFilters}>
          <X className="h-4 w-4 mr-1" />
          Clear Filters
        </Button>
      )}
    </div>
  )
}