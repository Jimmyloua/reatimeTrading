import apiClient from './client'
import type { User, UserProfileResponse, UpdateProfileRequest } from '@/types/user'

const API_BASE = '/api/users'

export const userApi = {
  /**
   * Get the current user's profile
   */
  async getProfile(): Promise<User> {
    const response = await apiClient.get<User>(`${API_BASE}/me`)
    return response.data
  },

  /**
   * Update the current user's profile
   */
  async updateProfile(data: UpdateProfileRequest): Promise<User> {
    const response = await apiClient.put<User>(`${API_BASE}/me`, data)
    return response.data
  },

  /**
   * Get a user's public profile by ID
   */
  async getUserById(id: number): Promise<UserProfileResponse> {
    const response = await apiClient.get<UserProfileResponse>(`${API_BASE}/${id}`)
    return response.data
  },

  /**
   * Upload a new avatar image
   */
  async uploadAvatar(file: File): Promise<{ avatarUrl: string }> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post<{ avatarUrl: string }>(
      `${API_BASE}/me/avatar`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    )
    return response.data
  },

  /**
   * Delete the current user's avatar
   */
  async deleteAvatar(): Promise<void> {
    await apiClient.delete(`${API_BASE}/me/avatar`)
  },
}