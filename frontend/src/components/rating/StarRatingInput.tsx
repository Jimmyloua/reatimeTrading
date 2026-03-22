import { useState } from 'react'
import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

interface StarRatingInputProps {
  value: number | null
  onChange: (rating: number) => void
  size?: 'md' | 'lg'
  disabled?: boolean
  className?: string
}

const SIZE_CONFIG = {
  md: 'w-8 h-8',
  lg: 'w-10 h-10',
}

export function StarRatingInput({
  value,
  onChange,
  size = 'lg',
  disabled = false,
  className,
}: StarRatingInputProps) {
  const [hovered, setHovered] = useState<number | null>(null)

  const displayValue = hovered ?? value ?? 0

  return (
    <div
      className={cn('flex gap-1', className)}
      role="radiogroup"
      aria-label="Rating"
    >
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={disabled}
          className={cn(
            SIZE_CONFIG[size],
            'rounded focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500',
            disabled && 'cursor-not-allowed opacity-50'
          )}
          onMouseEnter={() => !disabled && setHovered(star)}
          onMouseLeave={() => !disabled && setHovered(null)}
          onClick={() => !disabled && onChange(star)}
          aria-label={`${star} star${star > 1 ? 's' : ''}`}
          aria-checked={value === star}
          role="radio"
        >
          <Star
            className={cn(
              'w-full h-full transition-colors',
              star <= displayValue
                ? 'fill-amber-400 text-amber-400'
                : 'fill-zinc-200 text-zinc-200 hover:fill-amber-200 hover:text-amber-200'
            )}
          />
        </button>
      ))}
    </div>
  )
}