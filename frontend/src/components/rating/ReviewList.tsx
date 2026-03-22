import { Card } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { StarRatingDisplay } from './StarRatingDisplay'
import type { Rating } from '@/types/rating'
import { formatDistanceToNow } from 'date-fns'

interface ReviewListProps {
  ratings: Rating[]
}

export function ReviewList({ ratings }: ReviewListProps) {
  if (ratings.length === 0) {
    return (
      <p className="text-zinc-400 text-center py-8">
        No reviews yet
      </p>
    )
  }

  return (
    <div className="space-y-4">
      {ratings.map((rating) => (
        <Card key={rating.id} className="p-4">
          <div className="flex items-start gap-3">
            <Avatar className="w-10 h-10">
              <AvatarFallback>
                {rating.raterName?.charAt(0) || '?'}
              </AvatarFallback>
            </Avatar>
            <div className="flex-1">
              <div className="flex items-center justify-between">
                <span className="font-medium">{rating.raterName}</span>
                <span className="text-xs text-zinc-400">
                  {formatDistanceToNow(new Date(rating.createdAt), { addSuffix: true })}
                </span>
              </div>
              <StarRatingDisplay rating={rating.rating} size="sm" className="mt-1" />
              {rating.reviewText && (
                <p className="text-zinc-600 mt-2">{rating.reviewText}</p>
              )}
            </div>
          </div>
        </Card>
      ))}
    </div>
  )
}