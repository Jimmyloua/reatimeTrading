import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { userApi } from '@/api/userApi'
import { useAuthStore } from '@/stores/authStore'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Pencil, Loader2, X } from 'lucide-react'
import { toast } from 'sonner'
import { getInitials, getAvatarColor } from '@/pages/ProfilePage'

// Avatar size constants
const AVATAR_SIZE = 120 // 120x120 pixels for profile page

// Validation constants (from CONTEXT.md D-10, D-11)
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5 MB

interface AvatarUploadProps {
  currentAvatarUrl: string | null
  displayName: string | null
  email: string
  userId: number
  onUploadComplete: (url: string) => void
  size?: 'sm' | 'default' | 'lg'
}

export function AvatarUpload({
  currentAvatarUrl,
  displayName,
  email,
  userId,
  onUploadComplete,
  size = 'lg',
}: AvatarUploadProps) {
  const [isUploading, setIsUploading] = useState(false)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const { setUser, user } = useAuthStore()

  // Get initials for fallback
  const initials = getInitials(displayName, email)
  const bgColor = getAvatarColor(userId)

  // Handle file drop/selection
  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      const file = acceptedFiles[0]
      if (!file) return

      // Validate file size
      if (file.size > MAX_FILE_SIZE) {
        toast.error('File size must be less than 5 MB')
        return
      }

      // Create preview
      const objectUrl = URL.createObjectURL(file)
      setPreviewUrl(objectUrl)

      // Upload file
      setIsUploading(true)
      try {
        const result = await userApi.uploadAvatar(file)

        // Update auth store with new avatar URL
        if (user) {
          setUser({ ...user, avatarUrl: result.avatarUrl })
        }

        // Notify parent component
        onUploadComplete(result.avatarUrl)
        toast.success('Avatar uploaded successfully')

        // Clean up preview URL
        URL.revokeObjectURL(objectUrl)
        setPreviewUrl(null)
      } catch (error) {
        toast.error(
          error instanceof Error ? error.message : 'Failed to upload avatar'
        )
        // Clean up preview URL on error
        URL.revokeObjectURL(objectUrl)
        setPreviewUrl(null)
      } finally {
        setIsUploading(false)
      }
    },
    [onUploadComplete, setUser, user]
  )

  // Configure dropzone
  const { getRootProps, getInputProps, isDragActive, isDragReject } =
    useDropzone({
      onDrop,
      accept: {
        'image/jpeg': ['.jpg', '.jpeg'],
        'image/png': ['.png'],
        'image/webp': ['.webp'],
      },
      maxSize: MAX_FILE_SIZE,
      maxFiles: 1,
      disabled: isUploading,
    })

  // Get the current avatar URL (preview or stored)
  const displayUrl = previewUrl || currentAvatarUrl

  return (
    <div
      {...getRootProps()}
      className={`relative inline-flex cursor-pointer items-center justify-center ${
        isDragActive ? 'opacity-80' : ''
      } ${isDragReject ? 'ring-2 ring-destructive' : ''}`}
    >
      <input {...getInputProps()} />

      {/* Avatar Display */}
      <div
        className="relative group"
        style={{ width: AVATAR_SIZE, height: AVATAR_SIZE }}
      >
        {displayUrl ? (
          <Avatar
            size={size}
            className="h-full w-full ring-2 ring-border"
            style={{ width: AVATAR_SIZE, height: AVATAR_SIZE }}
          >
            <AvatarImage src={displayUrl} alt={displayName || 'User'} />
            <AvatarFallback>{initials}</AvatarFallback>
          </Avatar>
        ) : (
          <div
            className="flex h-full w-full items-center justify-center rounded-full font-semibold text-white"
            style={{ backgroundColor: bgColor, fontSize: '2rem' }}
          >
            {initials}
          </div>
        )}

        {/* Loading overlay */}
        {isUploading && (
          <div className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50">
            <Loader2 className="h-8 w-8 animate-spin text-white" />
          </div>
        )}

        {/* Edit overlay on hover */}
        {!isUploading && (
          <div className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
            {isDragActive ? (
              <span className="text-sm text-white">Drop image</span>
            ) : (
              <Pencil className="h-6 w-6 text-white" />
            )}
          </div>
        )}

        {/* Drag reject indicator */}
        {isDragReject && (
          <div className="absolute inset-0 flex items-center justify-center rounded-full bg-destructive/80">
            <X className="h-8 w-8 text-white" />
          </div>
        )}
      </div>

      {/* Hint text */}
      <p className="mt-2 text-xs text-muted-foreground">
        Click or drag to upload
      </p>
    </div>
  )
}