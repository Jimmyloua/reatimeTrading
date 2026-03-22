import { StarRatingDisplay } from './StarRatingDisplay'
import type { UserRatingSummary } from '@/types/rating'

interface RatingSummaryProps {
  summary: UserRatingSummary
  showCount?: boolean
}

export function RatingSummary({ summary, showCount = true }: RatingSummaryProps) {
  if (!summary.hasRatings) {
    return (
      <div className="text-zinc-400">
        No reviews yet
      </div>
    )
  }

  return (
    <div className="space-y-1">
      <div className="flex items-center gap-2">
        <span className="text-2xl font-bold">
          {summary.averageRating}
        </span>
        <StarRatingDisplay rating={parseFloat(summary.averageRating || '0')} size="sm" />
      </div>
      {showCount && (
        <p className="text-sm text-zinc-500">
          {summary.totalRatings} review{summary.totalRatings !== 1 ? 's' : ''}
        </p>
      )}
    </div>
  )
}