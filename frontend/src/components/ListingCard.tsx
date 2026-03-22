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
    <Card className={cn('group overflow-hidden border-white/60 bg-white/92 shadow-[0_16px_44px_rgba(15,23,42,0.08)] backdrop-blur-sm transition duration-300 hover:-translate-y-1 hover:shadow-[0_24px_60px_rgba(15,23,42,0.14)]', className)}>
      <Link to={`/listings/${id}`} className="block">
        {/* Image */}
        <div className="relative aspect-[1/0.9] bg-muted">
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
              'absolute top-3 right-3 rounded-full px-3 py-1 text-[11px] font-semibold shadow-lg',
              STATUS_COLORS[status]
            )}
          >
            {STATUS_LABELS[status]}
          </span>
          <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-slate-950/40 to-transparent" />
        </div>

        {/* Content */}
        <div className="space-y-3 p-4">
          {/* Title */}
          <h3 className="line-clamp-2 text-base font-semibold text-slate-900">{title}</h3>

          {/* Price */}
          <div className="flex items-center justify-between gap-3">
            <p className="text-xl font-semibold text-slate-950">{formatPrice(price)}</p>
            <span
              className={cn(
                'inline-block rounded-full px-3 py-1 text-xs font-semibold',
                CONDITION_COLORS[condition]
              )}
            >
              {CONDITION_LABELS[condition]}
            </span>
          </div>

          <div className="flex items-center justify-between gap-2">
            <p className="text-sm font-medium text-slate-500">{categoryName}</p>
            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Latest</p>
          </div>

          {/* Location */}
          {(city || region) && (
            <div className="flex items-center gap-1 text-sm text-slate-500">
              <MapPin className="h-3 w-3" />
              <span>{[city, region].filter(Boolean).join(', ')}</span>
            </div>
          )}

          {/* Created date */}
          <div className="flex items-center gap-1 text-sm text-slate-400">
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
