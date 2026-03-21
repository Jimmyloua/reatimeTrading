import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { CategorySelect } from '@/components/CategorySelect'
import { ImageUploader } from '@/components/ImageUploader'
import type { Category, ListingImage, Condition } from '@/types/listing'

// Validation schema
const listingSchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters').max(200, 'Title must be 200 characters or less'),
  description: z.string().min(1, 'Description is required').max(5000, 'Description must be 5000 characters or less'),
  price: z.number().positive('Price must be positive').max(99999999, 'Price is too high'),
  categoryId: z.number().min(1, 'Category is required'),
  condition: z.enum(['NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'POOR']),
  city: z.string().max(100).optional(),
  region: z.string().max(100).optional(),
})

type ListingFormData = z.infer<typeof listingSchema>

interface ListingFormProps {
  initialData?: Partial<ListingFormData>
  onSubmit: (data: ListingFormData, images: File[], primaryIndex: number) => Promise<void>
  isEditing?: boolean
  categories: Category[]
  existingImages?: ListingImage[]
  onDeleteImage?: (imageId: number) => void
  isLoading?: boolean
}

const CONDITION_OPTIONS: { value: Condition; label: string }[] = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
  { value: 'POOR', label: 'Poor' },
]

export function ListingForm({
  initialData,
  onSubmit,
  isEditing = false,
  categories,
  existingImages = [],
  onDeleteImage,
  isLoading = false,
}: ListingFormProps) {
  const [images, setImages] = useState<File[]>([])
  const [primaryIndex, setPrimaryIndex] = useState(0)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ListingFormData>({
    resolver: zodResolver(listingSchema),
    defaultValues: {
      title: initialData?.title || '',
      description: initialData?.description || '',
      price: initialData?.price || 0,
      categoryId: initialData?.categoryId,
      condition: initialData?.condition || 'NEW',
      city: initialData?.city || '',
      region: initialData?.region || '',
    },
  })

  const selectedCategoryId = watch('categoryId')

  const handleFormSubmit = async (data: ListingFormData) => {
    setIsSubmitting(true)
    try {
      await onSubmit(data, images, primaryIndex)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleCategoryChange = (categoryId: number | undefined) => {
    setValue('categoryId', categoryId ?? 0, { shouldValidate: true })
  }

  // Convert existing images to format expected by ImageUploader
  const formattedExistingImages = existingImages.map((img) => ({
    id: img.id,
    imageUrl: img.imageUrl,
    isPrimary: img.isPrimary,
  }))

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      {/* Title */}
      <div className="space-y-2">
        <Label htmlFor="title">Title</Label>
        <Input
          id="title"
          type="text"
          placeholder="What are you selling?"
          {...register('title')}
          disabled={isLoading || isSubmitting}
        />
        {errors.title && (
          <p className="text-sm text-destructive">{errors.title.message}</p>
        )}
      </div>

      {/* Description */}
      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <textarea
          id="description"
          placeholder="Describe your item in detail..."
          rows={5}
          {...register('description')}
          disabled={isLoading || isSubmitting}
          className="flex w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 md:text-sm"
        />
        {errors.description && (
          <p className="text-sm text-destructive">{errors.description.message}</p>
        )}
      </div>

      {/* Price */}
      <div className="space-y-2">
        <Label htmlFor="price">Price ($)</Label>
        <Input
          id="price"
          type="number"
          step="0.01"
          min="0"
          placeholder="0.00"
          {...register('price', { valueAsNumber: true })}
          disabled={isLoading || isSubmitting}
        />
        {errors.price && (
          <p className="text-sm text-destructive">{errors.price.message}</p>
        )}
      </div>

      {/* Category */}
      <div className="space-y-2">
        <Label htmlFor="categoryId">Category</Label>
        <CategorySelect
          value={selectedCategoryId}
          onChange={handleCategoryChange}
          categories={categories}
          placeholder="Select a category"
          disabled={isLoading || isSubmitting}
        />
        {errors.categoryId && (
          <p className="text-sm text-destructive">{errors.categoryId.message}</p>
        )}
      </div>

      {/* Condition */}
      <div className="space-y-2">
        <Label htmlFor="condition">Condition</Label>
        <select
          id="condition"
          {...register('condition')}
          disabled={isLoading || isSubmitting}
          className="flex h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 md:text-sm"
        >
          {CONDITION_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        {errors.condition && (
          <p className="text-sm text-destructive">{errors.condition.message}</p>
        )}
      </div>

      {/* Location */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="city">City (optional)</Label>
          <Input
            id="city"
            type="text"
            placeholder="City"
            {...register('city')}
            disabled={isLoading || isSubmitting}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="region">Region (optional)</Label>
          <Input
            id="region"
            type="text"
            placeholder="State/Province"
            {...register('region')}
            disabled={isLoading || isSubmitting}
          />
        </div>
      </div>

      {/* Images */}
      <div className="space-y-2">
        <Label>Images</Label>
        <ImageUploader
          images={images}
          onImagesChange={setImages}
          primaryIndex={primaryIndex}
          onPrimaryChange={setPrimaryIndex}
          existingImages={formattedExistingImages}
          onDeleteExisting={onDeleteImage}
          disabled={isLoading || isSubmitting}
        />
      </div>

      {/* Submit Button */}
      <Button type="submit" disabled={isLoading || isSubmitting} className="w-full">
        {isSubmitting
          ? 'Saving...'
          : isEditing
            ? 'Save Changes'
            : 'Create Listing'}
      </Button>
    </form>
  )
}