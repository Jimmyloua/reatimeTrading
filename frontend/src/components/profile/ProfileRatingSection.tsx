import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { RatingSummary } from '@/components/rating/RatingSummary'
import { ReviewList } from '@/components/rating/ReviewList'
import { ratingApi } from '@/api/ratingApi'

interface ProfileRatingSectionProps {
  userId: number
}

export function ProfileRatingSection({ userId }: ProfileRatingSectionProps) {
  const { data: summary, isLoading: loadingSummary } = useQuery({
    queryKey: ['rating-summary', userId],
    queryFn: () => ratingApi.getRatingSummary(userId),
  })

  const { data: ratings, isLoading: loadingRatings } = useQuery({
    queryKey: ['recent-ratings', userId],
    queryFn: () => ratingApi.getRecentRatings(userId),
  })

  if (loadingSummary || loadingRatings) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Reviews</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-20 w-full" />
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Reviews</CardTitle>
        {summary?.hasRatings && (
          <Button variant="link" className="text-sm">
            View all reviews
          </Button>
        )}
      </CardHeader>
      <CardContent className="space-y-4">
        {summary && <RatingSummary summary={summary} />}

        {ratings && ratings.length > 0 && (
          <div className="pt-4 border-t">
            <h4 className="font-medium mb-3">Recent Reviews</h4>
            <ReviewList ratings={ratings} />
          </div>
        )}
      </CardContent>
    </Card>
  )
}