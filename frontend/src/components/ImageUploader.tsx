import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { ImagePlus, X, Star } from 'lucide-react'

const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10 MB per file
const DEFAULT_MAX_IMAGES = 10

interface ExistingImage {
  id: number
  imageUrl: string
  isPrimary: boolean
}

interface ImageUploaderProps {
  images: File[]
  onImagesChange: (files: File[]) => void
  primaryIndex: number
  onPrimaryChange: (index: number) => void
  maxImages?: number
  existingImages?: ExistingImage[]
  onDeleteExisting?: (imageId: number) => void
  disabled?: boolean
}

export function ImageUploader({
  images,
  onImagesChange,
  primaryIndex,
  onPrimaryChange,
  maxImages = DEFAULT_MAX_IMAGES,
  existingImages = [],
  onDeleteExisting,
  disabled = false,
}: ImageUploaderProps) {
  const [previews, setPreviews] = useState<string[]>([])

  const totalImages = existingImages.length + images.length

  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      if (disabled) return

      const remainingSlots = maxImages - existingImages.length - images.length
      const filesToAdd = acceptedFiles.slice(0, remainingSlots)

      if (filesToAdd.length === 0) {
        return
      }

      // Create preview URLs
      const newPreviews = filesToAdd.map((file) => URL.createObjectURL(file))
      setPreviews((prev) => [...prev, ...newPreviews])

      // Add files to state
      onImagesChange([...images, ...filesToAdd])
    },
    [disabled, existingImages.length, images, maxImages, onImagesChange]
  )

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop,
    accept: {
      'image/jpeg': ['.jpg', '.jpeg'],
      'image/png': ['.png'],
      'image/webp': ['.webp'],
    },
    maxSize: MAX_FILE_SIZE,
    disabled: disabled || totalImages >= maxImages,
  })

  const removeNewImage = (index: number) => {
    const newImages = [...images]
    newImages.splice(index, 1)
    onImagesChange(newImages)

    // Clean up preview URL
    const newPreviews = [...previews]
    URL.revokeObjectURL(newPreviews[index])
    newPreviews.splice(index, 1)
    setPreviews(newPreviews)

    // Adjust primary index if needed
    if (primaryIndex >= existingImages.length + index) {
      onPrimaryChange(Math.max(0, primaryIndex - 1))
    }
  }

  const removeExistingImage = (imageId: number, index: number) => {
    if (onDeleteExisting) {
      onDeleteExisting(imageId)
      // Adjust primary index if needed
      if (primaryIndex === index) {
        onPrimaryChange(0)
      } else if (primaryIndex > index) {
        onPrimaryChange(primaryIndex - 1)
      }
    }
  }

  return (
    <div className="space-y-4">
      {/* Dropzone */}
      <div
        {...getRootProps()}
        className={cn(
          'flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-6 transition-colors cursor-pointer',
          isDragActive && 'border-primary bg-primary/5',
          isDragReject && 'border-destructive bg-destructive/5',
          (disabled || totalImages >= maxImages) && 'opacity-50 cursor-not-allowed',
          !isDragActive && !isDragReject && 'border-muted-foreground/25 hover:border-muted-foreground/50'
        )}
      >
        <input {...getInputProps()} />
        <ImagePlus className="h-10 w-10 text-muted-foreground mb-2" />
        {isDragActive ? (
          <p className="text-sm text-muted-foreground">Drop images here...</p>
        ) : totalImages >= maxImages ? (
          <p className="text-sm text-muted-foreground">Maximum {maxImages} images reached</p>
        ) : (
          <div className="text-center">
            <p className="text-sm text-muted-foreground">
              Drag and drop images, or click to select
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              PNG, JPG, or WebP (max 10MB each)
            </p>
          </div>
        )}
      </div>

      {/* Image count */}
      <p className="text-sm text-muted-foreground">
        {totalImages} / {maxImages} images
      </p>

      {/* Image grid */}
      {totalImages > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {/* Existing images */}
          {existingImages.map((image, index) => (
            <div
              key={image.id}
              className={cn(
                'relative aspect-square rounded-lg overflow-hidden border-2 transition-colors',
                image.isPrimary ? 'border-primary' : 'border-transparent'
              )}
            >
              <img
                src={image.imageUrl}
                alt={`Image ${index + 1}`}
                className="w-full h-full object-cover"
              />
              {/* Overlay with actions */}
              <div className="absolute inset-0 bg-black/50 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                {!image.isPrimary && (
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => onDeleteExisting?.(image.id)}
                  >
                    <Star className="h-4 w-4" />
                  </Button>
                )}
                <Button
                  type="button"
                  variant="destructive"
                  size="sm"
                  onClick={() => removeExistingImage(image.id, index)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              {/* Primary badge */}
              {image.isPrimary && (
                <div className="absolute top-1 left-1 bg-primary text-primary-foreground text-xs px-2 py-0.5 rounded">
                  Primary
                </div>
              )}
            </div>
          ))}

          {/* New images */}
          {images.map((_file, index) => {
            const globalIndex = existingImages.length + index
            const previewUrl = previews[index]
            return (
              <div
                key={`new-${index}`}
                className={cn(
                  'relative aspect-square rounded-lg overflow-hidden border-2 transition-colors',
                  primaryIndex === globalIndex ? 'border-primary' : 'border-transparent'
                )}
              >
                {previewUrl && (
                  <img
                    src={previewUrl}
                    alt={`New image ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                )}
                {/* Overlay with actions */}
                <div className="absolute inset-0 bg-black/50 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  {primaryIndex !== globalIndex && (
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={() => onPrimaryChange(globalIndex)}
                    >
                      <Star className="h-4 w-4" />
                    </Button>
                  )}
                  <Button
                    type="button"
                    variant="destructive"
                    size="sm"
                    onClick={() => removeNewImage(index)}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
                {/* Primary badge */}
                {primaryIndex === globalIndex && (
                  <div className="absolute top-1 left-1 bg-primary text-primary-foreground text-xs px-2 py-0.5 rounded">
                    Primary
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}