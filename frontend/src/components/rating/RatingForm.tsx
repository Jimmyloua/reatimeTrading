import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { StarRatingInput } from './StarRatingInput'
import { useMutation } from '@tanstack/react-query'
import { ratingApi } from '@/api/ratingApi'
import { toast } from 'sonner'
import { useNavigate } from 'react-router-dom'

interface RatingFormProps {
  transactionId: number
  otherPartyName: string
  userRole: 'BUYER' | 'SELLER'
  onSuccess?: () => void
}

export function RatingForm({
  transactionId,
  otherPartyName,
  userRole,
  onSuccess,
}: RatingFormProps) {
  const [rating, setRating] = useState<number | null>(null)
  const [reviewText, setReviewText] = useState('')
  const navigate = useNavigate()

  const submitMutation = useMutation({
    mutationFn: () =>
      ratingApi.submitRating(transactionId, {
        rating: rating!,
        reviewText: reviewText.trim() || undefined,
      }),
    onSuccess: () => {
      toast.success('Thank you for your review!')
      onSuccess?.()
      navigate(`/transactions/${transactionId}`)
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Failed to submit review'
      toast.error(message)
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (rating === null) {
      toast.error('Please select a rating')
      return
    }
    submitMutation.mutate()
  }

  const getRatingLabel = (value: number): string => {
    switch (value) {
      case 5: return 'Excellent'
      case 4: return 'Good'
      case 3: return 'Average'
      case 2: return 'Poor'
      case 1: return 'Terrible'
      default: return ''
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold mb-2">Rate Your Experience</h2>
        <p className="text-zinc-500">
          How was your experience with {otherPartyName} as a {userRole === 'BUYER' ? 'seller' : 'buyer'}?
        </p>
      </div>

      <div>
        <Label className="mb-2 block">Rating (1-5 stars) *</Label>
        <StarRatingInput
          value={rating}
          onChange={setRating}
          disabled={submitMutation.isPending}
        />
        {rating && (
          <p className="text-sm text-zinc-500 mt-1">
            {getRatingLabel(rating)}
          </p>
        )}
      </div>

      <div>
        <Label htmlFor="review">Review (optional)</Label>
        <Textarea
          id="review"
          value={reviewText}
          onChange={(e) => setReviewText(e.target.value)}
          placeholder="Share your experience with this transaction..."
          maxLength={500}
          rows={4}
          disabled={submitMutation.isPending}
        />
        <p className="text-xs text-zinc-400 mt-1">
          {reviewText.length}/500 characters
        </p>
      </div>

      <Button
        type="submit"
        className="w-full"
        disabled={rating === null || submitMutation.isPending}
      >
        {submitMutation.isPending ? 'Submitting...' : 'Submit Review'}
      </Button>

      <p className="text-xs text-zinc-400 text-center">
        Your review will be visible once the other party submits their review or after 14 days.
      </p>
    </form>
  )
}