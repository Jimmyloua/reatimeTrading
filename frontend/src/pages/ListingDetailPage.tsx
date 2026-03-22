import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { listingApi } from '@/api/listingApi'
import { useAuthStore } from '@/stores/authStore'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { ArrowLeft, Pencil, Trash2, MapPin, Clock } from 'lucide-react'
import { getInitials, getAvatarColor } from './ProfilePage'
import { RequestToBuyButton } from '@/components/transaction/RequestToBuyButton'
import type { Condition, ListingStatus } from '@/types/listing'

// Condition colors and labels
const CONDITION_COLORS: Record<Condition, string> = {
  NEW: 'bg-green-100 text-green-800',
  LIKE_NEW: 'bg-blue-100 text-blue-800',
  GOOD: 'bg-yellow-100 text-yellow-800',
  FAIR: 'bg-orange-100 text-orange-800',
  POOR: 'bg-red-100 text-red-800',
}

const CONDITION_LABELS: Record<Condition, string> = {
  NEW: 'New',
  LIKE_NEW: 'Like New',
  GOOD: 'Good',
  FAIR: 'Fair',
  POOR: 'Poor',
}

const STATUS_COLORS: Record<ListingStatus, string> = {
  AVAILABLE: 'bg-green-500 text-white',
  RESERVED: 'bg-yellow-500 text-white',
  SOLD: 'bg-gray-500 text-white',
}

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
 * Format date
 */
function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

export default function ListingDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user, isAuthenticated } = useAuthStore()
  const listingId = parseInt(id || '0', 10)

  // Fetch listing detail
  const {
    data: listing,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['listing', listingId],
    queryFn: () => listingApi.getListingDetail(listingId),
    enabled: !!listingId,
  })

  // Check if current user is the owner
  const isOwner = user && listing && user.id === listing.seller.id

  if (isLoading) {
    return (
      <div className="py-8 space-y-6">
        <Skeleton className="h-8 w-32" />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <Skeleton className="aspect-square rounded-lg" />
          <div className="space-y-4">
            <Skeleton className="h-8 w-3/4" />
            <Skeleton className="h-6 w-1/4" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        </div>
      </div>
    )
  }

  if (error || !listing) {
    return (
      <div className="py-8">
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-destructive">Listing not found</p>
            <Button
              variant="outline"
              className="mt-4"
              onClick={() => navigate('/listings')}
            >
              Browse Listings
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  const primaryImage = listing.images.find((img) => img.isPrimary) || listing.images[0]

  return (
    <div className="py-8 space-y-6">
      {/* Back button */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => navigate('/listings')}
      >
        <ArrowLeft className="h-4 w-4 mr-1" />
        Back to Listings
      </Button>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Image Gallery */}
        <div className="space-y-4">
          {/* Main Image */}
          <div className="relative aspect-square bg-muted rounded-lg overflow-hidden">
            {primaryImage ? (
              <img
                src={primaryImage.imageUrl}
                alt={listing.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-muted-foreground">
                No image
              </div>
            )}

            {/* Status badge */}
            <span
              className={`absolute top-4 right-4 px-3 py-1 rounded text-sm font-medium ${STATUS_COLORS[listing.status]}`}
            >
              {STATUS_LABELS[listing.status]}
            </span>
          </div>

          {/* Image Thumbnails */}
          {listing.images.length > 1 && (
            <div className="flex gap-2 overflow-x-auto pb-2">
              {listing.images.map((image) => (
                <button
                  key={image.id}
                  className={`flex-shrink-0 w-20 h-20 rounded overflow-hidden border-2 transition-colors ${
                    image.isPrimary ? 'border-primary' : 'border-transparent'
                  }`}
                >
                  <img
                    src={image.imageUrl}
                    alt=""
                    className="w-full h-full object-cover"
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Details */}
        <div className="space-y-6">
          {/* Title and Price */}
          <div>
            <h1 className="text-2xl font-semibold text-foreground">{listing.title}</h1>
            <p className="text-3xl font-bold text-foreground mt-2">{formatPrice(listing.price)}</p>
          </div>

          {/* Badges */}
          <div className="flex flex-wrap gap-2">
            <span
              className={`px-3 py-1 rounded text-sm font-medium ${CONDITION_COLORS[listing.condition]}`}
            >
              {CONDITION_LABELS[listing.condition]}
            </span>
          </div>

          {/* Location */}
          {(listing.city || listing.region) && (
            <div className="flex items-center gap-2 text-muted-foreground">
              <MapPin className="h-4 w-4" />
              <span>{[listing.city, listing.region].filter(Boolean).join(', ')}</span>
            </div>
          )}

          {/* Description */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Description</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground whitespace-pre-wrap">{listing.description}</p>
            </CardContent>
          </Card>

          {/* Dates */}
          <div className="flex items-center gap-4 text-sm text-muted-foreground">
            <div className="flex items-center gap-1">
              <Clock className="h-4 w-4" />
              <span>Posted {formatDate(listing.createdAt)}</span>
            </div>
            {listing.updatedAt && listing.updatedAt !== listing.createdAt && (
              <span>Updated {formatDate(listing.updatedAt)}</span>
            )}
          </div>

          {/* Seller Info */}
          <Card>
            <CardContent className="py-4">
              <div className="flex items-center gap-4">
                {listing.seller.avatarUrl ? (
                  <Avatar>
                    <AvatarImage src={listing.seller.avatarUrl} alt={listing.seller.displayName} />
                    <AvatarFallback>{getInitials(listing.seller.displayName, 'U')}</AvatarFallback>
                  </Avatar>
                ) : (
                  <div
                    className="flex h-10 w-10 items-center justify-center rounded-full text-sm font-semibold text-white"
                    style={{ backgroundColor: getAvatarColor(listing.seller.id) }}
                  >
                    {getInitials(listing.seller.displayName, 'U')}
                  </div>
                )}
                <div className="flex-1">
                  <Link
                    to={`/users/${listing.seller.id}`}
                    className="font-medium text-foreground hover:underline"
                  >
                    {listing.seller.displayName || 'Anonymous'}
                  </Link>
                  <p className="text-sm text-muted-foreground">
                    Member since {formatDate(listing.seller.memberSince)}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {listing.seller.listingCount} {listing.seller.listingCount === 1 ? 'listing' : 'listings'}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Actions */}
          {isOwner ? (
            <div className="flex gap-4">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => navigate(`/listings/${listingId}/edit`)}
              >
                <Pencil className="h-4 w-4 mr-2" />
                Edit Listing
              </Button>
              <Button
                variant="destructive"
                className="flex-1"
                onClick={() => navigate(`/listings/${listingId}/edit`)}
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete
              </Button>
            </div>
          ) : isAuthenticated && listing.status === 'AVAILABLE' ? (
            <RequestToBuyButton
              listingId={listingId}
              sellerId={listing.seller.id}
              isOwner={false}
            />
          ) : null}
        </div>
      </div>
    </div>
  )
}