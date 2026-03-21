import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useUserProfile } from '@/hooks/useUserProfile'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Skeleton } from '@/components/ui/skeleton'
import { Pencil } from 'lucide-react'

// Curated color palette for default avatars (from UI-SPEC.md)
const AVATAR_COLORS = [
  '#3b82f6', // blue
  '#22c55e', // green
  '#a855f7', // purple
  '#f97316', // orange
  '#ec4899', // pink
  '#14b8a6', // teal
]

/**
 * Generate a consistent color for a user based on their ID or email
 */
function getAvatarColor(identifier: string | number): string {
  const str = String(identifier)
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length]
}

/**
 * Get initials for avatar fallback
 * Uses first letter of displayName or first letter of email
 */
function getInitials(displayName: string | null, email: string): string {
  if (displayName) {
    // Get first letter of each word, max 2 letters
    const words = displayName.trim().split(/\s+/)
    if (words.length >= 2) {
      return (words[0][0] + words[1][0]).toUpperCase()
    }
    return displayName[0].toUpperCase()
  }
  // Fallback to first letter of email
  return email[0].toUpperCase()
}

/**
 * Default Avatar component with initials
 */
function DefaultAvatar({
  displayName,
  email,
  identifier,
  size = 'lg',
}: {
  displayName: string | null
  email: string
  identifier: string | number
  size?: 'sm' | 'default' | 'lg'
}) {
  const initials = getInitials(displayName, email)
  const bgColor = getAvatarColor(identifier)

  return (
    <Avatar size={size} className="bg-muted">
      <AvatarFallback
        className="font-semibold text-white"
        style={{ backgroundColor: bgColor }}
      >
        {initials}
      </AvatarFallback>
    </Avatar>
  )
}

/**
 * Format date to readable string
 */
function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

export default function ProfilePage() {
  const navigate = useNavigate()
  const { profile, isLoading, error, updateProfile, isUpdating } = useUserProfile()
  const [displayName, setDisplayName] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)

  // Sync local state with profile data
  useEffect(() => {
    if (profile) {
      setDisplayName(profile.displayName || '')
    }
  }, [profile])

  // Handle display name change
  const handleDisplayNameChange = (value: string) => {
    setDisplayName(value)
    // Validate: max 50 characters (UI-SPEC.md)
    if (value.length > 50) {
      setValidationError('Display name must be 50 characters or less')
    } else {
      setValidationError(null)
    }
  }

  // Handle form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    // Validate before submission
    if (displayName.length > 50) {
      setValidationError('Display name must be 50 characters or less')
      return
    }

    updateProfile({
      displayName: displayName.trim() || '',
    })
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Card>
          <CardHeader>
            <Skeleton className="h-8 w-32" />
            <Skeleton className="h-4 w-48" />
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex justify-center">
              <Skeleton className="h-32 w-32 rounded-full" />
            </div>
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-24" />
          </CardContent>
        </Card>
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-destructive">Failed to load profile</p>
            <Button
              variant="outline"
              className="mt-4"
              onClick={() => navigate('/')}
            >
              Go Home
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  // No profile data
  if (!profile) {
    return null
  }

  const displayDisplayName = profile.displayName || 'New User'

  return (
    <div className="mx-auto max-w-2xl py-8">
      <Card>
        <CardHeader>
          <CardTitle>Profile</CardTitle>
          <CardDescription>
            Manage your profile information
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Avatar Section */}
            <div className="flex justify-center">
              <div className="relative group">
                {profile.avatarUrl ? (
                  <Avatar size="lg" className="h-32 w-32">
                    <AvatarImage
                      src={profile.avatarUrl}
                      alt={displayDisplayName}
                    />
                    <AvatarFallback>
                      {getInitials(profile.displayName, profile.email)}
                    </AvatarFallback>
                  </Avatar>
                ) : (
                  <DefaultAvatar
                    displayName={profile.displayName}
                    email={profile.email}
                    identifier={profile.id}
                    size="lg"
                  />
                )}
                {/* Edit overlay - will be connected to AvatarUpload in Task 2 */}
                <div className="absolute inset-0 flex cursor-pointer items-center justify-center rounded-full bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
                  <Pencil className="h-6 w-6 text-white" />
                </div>
              </div>
            </div>

            {/* Display Name Field */}
            <div className="space-y-2">
              <Label htmlFor="displayName">Display Name</Label>
              <Input
                id="displayName"
                type="text"
                placeholder="Enter your display name"
                value={displayName}
                onChange={(e) => handleDisplayNameChange(e.target.value)}
                maxLength={100}
              />
              {validationError && (
                <p className="text-sm text-destructive">{validationError}</p>
              )}
              <p className="text-xs text-muted-foreground">
                This is how others will see you on the platform
              </p>
            </div>

            {/* Join Date - Read Only */}
            <div className="space-y-2">
              <Label>Member Since</Label>
              <p className="text-sm text-muted-foreground">
                {formatDate(profile.createdAt)}
              </p>
            </div>

            {/* Listing Count - Read Only */}
            <div className="space-y-2">
              <Label>Listings</Label>
              <p className="text-sm text-muted-foreground">
                {profile.listingCount} {profile.listingCount === 1 ? 'listing' : 'listings'}
              </p>
            </div>

            {/* Submit Button */}
            <Button type="submit" disabled={isUpdating || !!validationError}>
              {isUpdating ? 'Saving...' : 'Save profile'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

// Export helper functions for use in other components
export { getInitials, getAvatarColor, DefaultAvatar }