import { useParams, useNavigate, Link } from 'react-router-dom'
import { usePublicProfile } from '@/hooks/useUserProfile'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ArrowLeft, User } from 'lucide-react'
import { DefaultAvatar } from '@/pages/ProfilePage'
import { ProfileRatingSection } from '@/components/profile/ProfileRatingSection'

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

export default function UserProfilePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const userId = id ? parseInt(id, 10) : undefined

  const { profile, isLoading, error } = usePublicProfile(userId)

  // Loading state
  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Button
          variant="ghost"
          className="mb-4"
          onClick={() => navigate(-1)}
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back
        </Button>
        <Card>
          <CardHeader>
            <Skeleton className="h-8 w-32" />
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex justify-center">
              <Skeleton className="h-32 w-32 rounded-full" />
            </div>
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-4 w-24" />
          </CardContent>
        </Card>
      </div>
    )
  }

  // Error state (includes 404 user not found)
  if (error || !profile) {
    return (
      <div className="mx-auto max-w-2xl py-8">
        <Button
          variant="ghost"
          className="mb-4"
          onClick={() => navigate(-1)}
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back
        </Button>
        <Card>
          <CardContent className="py-8 text-center">
            <User className="mx-auto h-12 w-12 text-muted-foreground" />
            <p className="mt-4 text-lg font-medium text-foreground">
              User not found
            </p>
            <p className="mt-2 text-sm text-muted-foreground">
              This user may not exist or their profile is not available.
            </p>
            <Link to="/">
              <Button variant="outline" className="mt-4">
                Go Home
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    )
  }

  const displayDisplayName = profile.displayName || 'New User'

  return (
    <div className="mx-auto max-w-2xl py-8">
      <Button
        variant="ghost"
        className="mb-4"
        onClick={() => navigate(-1)}
      >
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>User Profile</CardTitle>
          <CardDescription>
            View {displayDisplayName}'s profile
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-6">
            {/* Avatar Section */}
            <div className="flex justify-center">
              {profile.avatarUrl ? (
                <div className="flex h-32 w-32 items-center justify-center overflow-hidden rounded-full ring-2 ring-border">
                  <img
                    src={profile.avatarUrl}
                    alt={displayDisplayName}
                    className="h-full w-full object-cover"
                  />
                </div>
              ) : (
                <DefaultAvatar
                  displayName={profile.displayName}
                  email={profile.email || ''}
                  identifier={profile.id}
                  size="lg"
                />
              )}
            </div>

            {/* Display Name */}
            <div className="text-center">
              <h2 className="text-xl font-semibold text-foreground">
                {displayDisplayName}
              </h2>
              {profile.displayName && (
                <p className="text-sm text-muted-foreground">
                  @{profile.email?.split('@')[0]}
                </p>
              )}
            </div>

            {/* Join Date */}
            <div className="text-center">
              <p className="text-sm text-muted-foreground">
                Member since {formatDate(profile.createdAt)}
              </p>
            </div>

            {/* Listing Count */}
            <div className="text-center">
              <p className="text-sm text-muted-foreground">
                {profile.listingCount}{' '}
                {profile.listingCount === 1 ? 'listing' : 'listings'}
              </p>
            </div>

            {/* Ratings Section */}
            <ProfileRatingSection userId={profile.id} />
          </div>
        </CardContent>
      </Card>
    </div>
  )
}