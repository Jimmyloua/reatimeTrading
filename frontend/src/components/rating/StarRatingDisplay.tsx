import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

interface StarRatingDisplayProps {
  rating: number | null
  size?: 'sm' | 'md' | 'lg'
  showValue?: boolean
  className?: string
}

const SIZE_CONFIG = {
  sm: { star: 'w-4 h-4', text: 'text-sm' },
  md: { star: 'w-5 h-5', text: 'text-base' },
  lg: { star: 'w-6 h-6', text: 'text-lg' },
}

export function StarRatingDisplay({
  rating,
  size = 'md',
  showValue = false,
  className,
}: StarRatingDisplayProps) {
  const config = SIZE_CONFIG[size]

  if (rating === null || rating === undefined) {
    return (
      <span className={cn('text-zinc-400', config.text, className)}>
        No ratings yet
      </span>
    )
  }

  return (
    <div className={cn('flex items-center gap-1', className)}>
      <div className="flex">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            className={cn(
              config.star,
              star <= Math.round(rating)
                ? 'fill-amber-400 text-amber-400'
                : 'fill-zinc-200 text-zinc-200'
            )}
          />
        ))}
      </div>
      {showValue && (
        <span className={cn('ml-1 font-medium', config.text)}>
          {rating.toFixed(1)}
        </span>
      )}
    </div>
  )
}