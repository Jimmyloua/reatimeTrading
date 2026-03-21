import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listingApi } from '@/api/listingApi'
import { ListingForm } from '@/components/ListingForm'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import type { CreateListingRequest } from '@/types/listing'

export default function CreateListingPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  // Fetch categories
  const { data: categories = [], isLoading: isLoadingCategories } = useQuery({
    queryKey: ['categories'],
    queryFn: listingApi.getCategories,
  })

  // Create listing mutation
  const createListingMutation = useMutation({
    mutationFn: listingApi.createListing,
    onSuccess: async (listing) => {
      toast.success('Listing created successfully')
      // Invalidate relevant queries
      queryClient.invalidateQueries({ queryKey: ['listings'] })
      // Navigate to the listing detail page
      navigate(`/listings/${listing.id}`)
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to create listing')
    },
  })

  // Handle form submission
  const handleSubmit = async (
    data: CreateListingRequest,
    images: File[],
    primaryIndex: number
  ) => {
    // First create the listing
    const listing = await createListingMutation.mutateAsync(data)

    // Then upload images if any
    if (images.length > 0) {
      try {
        await listingApi.uploadImages(listing.id, images, primaryIndex)
      } catch (error) {
        toast.warning('Listing created, but image upload failed')
        console.error('Image upload error:', error)
      }
    }
  }

  if (isLoadingCategories) {
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

  return (
    <div className="mx-auto max-w-2xl py-8">
      <Card>
        <CardHeader>
          <CardTitle>Create New Listing</CardTitle>
          <CardDescription>
            List your item for sale. Add photos and details to attract buyers.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ListingForm
            onSubmit={handleSubmit}
            categories={categories}
            isLoading={createListingMutation.isPending}
          />
        </CardContent>
      </Card>
    </div>
  )
}