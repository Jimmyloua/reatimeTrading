export interface Rating {
  id: number
  transactionId: number
  raterId: number
  raterName: string
  ratedUserId: number
  rating: number // 1-5
  reviewText: string | null
  visible: boolean
  createdAt: string
}

export interface RatingRequest {
  rating: number // 1-5
  reviewText?: string
}

export interface UserRatingSummary {
  userId: number
  averageRating: string | null
  totalRatings: number
  hasRatings: boolean
}

export interface CanRateResponse {
  canRate: boolean
}