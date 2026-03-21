import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listingApi } from '@/api/listingApi'
import { ListingForm } from '@/components/ListingForm'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import { ArrowLeft, Trash2 } from 'lucide-react'
import type { UpdateListingRequest, ListingStatus } from '@/types/listing'

const STATUS_OPTIONS: { value: ListingStatus; label: string }[] = [
  { value: 'AVAILABLE', label: 'Available' },
  { value: 'RESERVED', label: 'Reserved' },
  { value: 'SOLD', label: 'Sold' },
]

export default function EditListingPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const listingId = parseInt(id || '0', 10)

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  // Fetch listing detail
  const {
    data: listing,
    isLoading: isLoadingListing,
    error: listingError,
  } = useQuery({
    queryKey: ['listing', listingId],
    queryFn: () => listingApi.getListingDetail(listingId),
    enabled: !!listingId,
  })

  // Fetch categories
  const { data: categories = [], isLoading: isLoadingCategories } = useQuery({
    queryKey: ['categories'],
    queryFn: listingApi.getCategories,
  })

  // Update listing mutation
  const updateListingMutation = useMutation({
    mutationFn: (data: UpdateListingRequest) => listingApi.updateListing(listingId, data),
    onSuccess: () => {
      toast.success('Listing updated successfully')
      queryClient.invalidateQueries({ queryKey: ['listing', listingId] })
      queryClient.invalidateQueries({ queryKey: ['listings'] })
      navigate(`/listings/${listingId}`)
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update listing')
    },
  })

  // Delete listing mutation
  const deleteListingMutation = useMutation({
    mutationFn: () => listingApi.deleteListing(listingId),
    onSuccess: () => {
      toast.success('Listing deleted successfully')
      queryClient.invalidateQueries({ queryKey: ['listings'] })
      navigate('/listings')
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to delete listing')
    },
  })

  // Update status mutation
  const updateStatusMutation = useMutation({
    mutationFn: (status: ListingStatus) => listingApi.updateStatus(listingId, status),
    onSuccess: () => {
      toast.success('Status updated successfully')
      queryClient.invalidateQueries({ queryKey: ['listing', listingId] })
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update status')
    },
  })

  // Delete image mutation
  const deleteImageMutation = useMutation({
    mutationFn: (imageId: number) => listingApi.deleteImage(listingId, imageId),
    onSuccess: () => {
      toast.success('Image deleted')
      queryClient.invalidateQueries({ queryKey: ['listing', listingId] })
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to delete image')
    },
  })

  // Handle form submission
  const handleSubmit = async (
    data: UpdateListingRequest,
    images: File[],
    primaryIndex: number
  ) => {
    await updateListingMutation.mutateAsync(data)

    // Upload new images if any
    if (images.length > 0) {
      try {
        await listingApi.uploadImages(listingId, images, primaryIndex)
      } catch (error) {
        toast.warning('Listing updated, but image upload failed')
        console.error('Image upload error:', error)
      }
    }
  }

  // Handle delete image
  const handleDeleteImage = (imageId: number) => {
    deleteImageMutation.mutate(imageId)
  }

  // Handle status change
  const handleStatusChange = (status: ListingStatus) => {
    updateStatusMutation.mutate(status)
  }

  // Handle delete listing
  const handleDelete = () => {
    deleteListingMutation.mutate()
  }

  const isLoading = isLoadingListing || isLoadingCategories

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Card>
          <CardHeader>
            <Skeleton className="h-8 w-40" />
            <Skeleton className="h-4 w-60" />
          </CardHeader>
          <CardContent className="space-y-6">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-24" />
          </CardContent>
        </Card>
      </div>
    )
  }

  if (listingError || !listing) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-destructive">Failed to load listing</p>
            <Button
              variant="outline"
              className="mt-4"
              onClick={() => navigate('/listings')}
            >
              Go to Listings
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl py-8">
      {/* Back button */}
      <Button
        variant="ghost"
        size="sm"
        className="mb-4"
        onClick={() => navigate(`/listings/${listingId}`)}
      >
        <ArrowLeft className="h-4 w-4 mr-1" />
        Back to Listing
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>Edit Listing</CardTitle>
          <CardDescription>
            Update your listing details. Changes will be visible immediately.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {/* Status selector */}
          <div className="mb-6 space-y-2">
            <label className="text-sm font-medium">Status</label>
            <div className="flex gap-2">
              {STATUS_OPTIONS.map((option) => (
                <Button
                  key={option.value}
                  type="button"
                  variant={listing.status === option.value ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => handleStatusChange(option.value)}
                  disabled={updateStatusMutation.isPending}
                >
                  {option.label}
                </Button>
              ))}
            </div>
          </div>

          <ListingForm
            initialData={{
              title: listing.title,
              description: listing.description,
              price: listing.price,
              categoryId: listing.categoryId,
              condition: listing.condition,
              city: listing.city,
              region: listing.region,
            }}
            onSubmit={handleSubmit}
            isEditing
            categories={categories}
            existingImages={listing.images}
            onDeleteImage={handleDeleteImage}
            isLoading={updateListingMutation.isPending}
          />
        </CardContent>
      </Card>

      {/* Delete section */}
      <Card className="mt-6 border-destructive/50">
        <CardHeader>
          <CardTitle className="text-destructive">Delete Listing</CardTitle>
          <CardDescription>
            Once deleted, this listing cannot be recovered. This action is permanent.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {showDeleteConfirm ? (
            <div className="space-y-4">
              <p className="text-sm text-muted-foreground">
                Are you sure you want to delete "{listing.title}"?
              </p>
              <div className="flex gap-2">
                <Button
                  variant="destructive"
                  onClick={handleDelete}
                  disabled={deleteListingMutation.isPending}
                >
                  {deleteListingMutation.isPending ? 'Deleting...' : 'Yes, Delete'}
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={deleteListingMutation.isPending}
                >
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <Button
              variant="destructive"
              onClick={() => setShowDeleteConfirm(true)}
            >
              <Trash2 className="h-4 w-4 mr-1" />
              Delete Listing
            </Button>
          )}
        </CardContent>
      </Card>
    </div>
  )
}