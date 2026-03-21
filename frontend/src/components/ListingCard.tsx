import { Link } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Pencil, Trash2, MapPin, Clock } from 'lucide-react'
import type { Listing, Condition, ListingStatus } from '@/types/listing'

interface ListingCardProps {
  listing: Listing
  showActions?: boolean
  onEdit?: () => void
  onDelete?: () => void
  className?: string
}

// Condition colors
const CONDITION_COLORS: Record<Condition, string> = {
  NEW: 'bg-green-100 text-green-800',
  LIKE_NEW: 'bg-blue-100 text-blue-800',
  GOOD: 'bg-yellow-100 text-yellow-800',
  FAIR: 'bg-orange-100 text-orange-800',
  POOR: 'bg-red-100 text-red-800',
}

// Status colors
const STATUS_COLORS: Record<ListingStatus, string> = {
  AVAILABLE: 'bg-green-500 text-white',
  RESERVED: 'bg-yellow-500 text-white',
  SOLD: 'bg-gray-500 text-white',
}

// Condition labels
const CONDITION_LABELS: Record<Condition, string> = {
  NEW: 'New',
  LIKE_NEW: 'Like New',
  GOOD: 'Good',
  FAIR: 'Fair',
  POOR: 'Poor',
}

// Status labels
const STATUS_LABELS: Record<ListingStatus, string> = {
  AVAILABLE: 'Available',
  RESERVED: 'Reserved',
  SOLD: 'Sold',
}

/**
 * Format price as currency
 */
function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(price)
}

/**
 * Format date as relative time
 */
function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diffInSeconds < 60) {
    return 'Just now'
  }

  const diffInMinutes = Math.floor(diffInSeconds / 60)
  if (diffInMinutes < 60) {
    return `${diffInMinutes} minute${diffInMinutes === 1 ? '' : 's'} ago`
  }

  const diffInHours = Math.floor(diffInMinutes / 60)
  if (diffInHours < 24) {
    return `${diffInHours} hour${diffInHours === 1 ? '' : 's'} ago`
  }

  const diffInDays = Math.floor(diffInHours / 24)
  if (diffInDays < 30) {
    return `${diffInDays} day${diffInDays === 1 ? '' : 's'} ago`
  }

  const diffInMonths = Math.floor(diffInDays / 30)
  if (diffInMonths < 12) {
    return `${diffInMonths} month${diffInMonths === 1 ? '' : 's'} ago`
  }

  const diffInYears = Math.floor(diffInMonths / 12)
  return `${diffInYears} year${diffInYears === 1 ? '' : 's'} ago`
}

export function ListingCard({
  listing,
  showActions = false,
  onEdit,
  onDelete,
  className,
}: ListingCardProps) {
  const { id, title, price, condition, status, city, region, primaryImageUrl, categoryName, createdAt } = listing

  return (
    <Card className={cn('group overflow-hidden transition-shadow hover:shadow-lg', className)}>
      <Link to={`/listings/${id}`} className="block">
        {/* Image */}
        <div className="relative aspect-square bg-muted">
          {primaryImageUrl ? (
            <img
              src={primaryImageUrl}
              alt={title}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-muted-foreground">
              No image
            </div>
          )}

          {/* Status badge */}
          <span
            className={cn(
              'absolute top-2 right-2 px-2 py-0.5 rounded text-xs font-medium',
              STATUS_COLORS[status]
            )}
          >
            {STATUS_LABELS[status]}
          </span>
        </div>

        {/* Content */}
        <div className="p-4 space-y-2">
          {/* Title */}
          <h3 className="font-medium text-foreground line-clamp-2">{title}</h3>

          {/* Price */}
          <p className="text-lg font-semibold text-foreground">{formatPrice(price)}</p>

          {/* Condition badge */}
          <span
            className={cn(
              'inline-block px-2 py-0.5 rounded text-xs font-medium',
              CONDITION_COLORS[condition]
            )}
          >
            {CONDITION_LABELS[condition]}
          </span>

          {/* Category */}
          <p className="text-sm text-muted-foreground">{categoryName}</p>

          {/* Location */}
          {(city || region) && (
            <div className="flex items-center gap-1 text-sm text-muted-foreground">
              <MapPin className="h-3 w-3" />
              <span>{[city, region].filter(Boolean).join(', ')}</span>
            </div>
          )}

          {/* Created date */}
          <div className="flex items-center gap-1 text-sm text-muted-foreground">
            <Clock className="h-3 w-3" />
            <span>{formatRelativeTime(createdAt)}</span>
          </div>
        </div>
      </Link>

      {/* Action buttons (for owner) */}
      {showActions && (
        <div className="flex gap-2 p-4 pt-0">
          <Button
            variant="outline"
            size="sm"
            onClick={(e) => {
              e.preventDefault()
              onEdit?.()
            }}
            className="flex-1"
          >
            <Pencil className="h-4 w-4 mr-1" />
            Edit
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={(e) => {
              e.preventDefault()
              onDelete?.()
            }}
            className="flex-1"
          >
            <Trash2 className="h-4 w-4 mr-1" />
            Delete
          </Button>
        </div>
      )}
    </Card>
  )
}