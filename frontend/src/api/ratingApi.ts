import apiClient from './client'
import type {
  Rating,
  RatingRequest,
  UserRatingSummary,
  CanRateResponse,
} from '@/types/rating'
import type { PaginatedResponse } from '@/types/listing'

const API_BASE = '/api/ratings'

export const ratingApi = {
  /**
   * Submit a rating for a transaction
   */
  async submitRating(transactionId: number, data: RatingRequest): Promise<Rating> {
    const response = await apiClient.post<Rating>(
      `${API_BASE}/transactions/${transactionId}`,
      data
    )
    return response.data
  },

  /**
   * Get paginated ratings for a user
   */
  async getUserRatings(
    userId: number,
    page = 0,
    size = 10
  ): Promise<PaginatedResponse<Rating>> {
    const response = await apiClient.get<PaginatedResponse<Rating>>(
      `${API_BASE}/users/${userId}`,
      { params: { page, size } }
    )
    return response.data
  },

  /**
   * Get recent ratings for user profile (last 5)
   */
  async getRecentRatings(userId: number): Promise<Rating[]> {
    const response = await apiClient.get<Rating[]>(
      `${API_BASE}/users/${userId}/recent`
    )
    return response.data
  },

  /**
   * Get rating summary for user profile
   */
  async getRatingSummary(userId: number): Promise<UserRatingSummary> {
    const response = await apiClient.get<UserRatingSummary>(
      `${API_BASE}/users/${userId}/summary`
    )
    return response.data
  },

  /**
   * Check if user can rate a transaction
   */
  async canRate(transactionId: number): Promise<boolean> {
    const response = await apiClient.get<CanRateResponse>(
      `${API_BASE}/transactions/${transactionId}/can-rate`
    )
    return response.data.canRate
  },
}