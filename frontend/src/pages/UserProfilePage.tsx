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
import { HERO_IMAGES, buildHeroBackground } from '@/lib/heroBackgrounds'

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

      <section
        className="relative mb-6 overflow-hidden rounded-[2.25rem] border border-white/40 px-6 py-8 shadow-[0_28px_90px_rgba(15,23,42,0.18)]"
        style={buildHeroBackground(HERO_IMAGES.profile)}
      >
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.18),transparent_28%)]" />
        <div className="relative">
          <p className="text-sm font-medium uppercase tracking-[0.28em] text-amber-200/85">Public profile</p>
          <h1 className="mt-3 text-3xl font-semibold text-white md:text-4xl">{displayDisplayName}</h1>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-200 md:text-base">
            Browse seller identity, reputation, and marketplace activity in the same visual style as listings and transactions.
          </p>
        </div>
      </section>

      <Card className="border-white/60 bg-white/92 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
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
