import apiClient from './client'
import type {
  LoginResponse,
  LoginRequest,
  RegisterRequest,
  RefreshRequest,
  LogoutRequest,
  UserProfileResponse,
} from '@/types/user'

export const authApi = {
  register: async (email: string, password: string): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/register', {
      email,
      password,
    } as RegisterRequest)
    return response.data
  },

  login: async (email: string, password: string): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', {
      email,
      password,
    } as LoginRequest)
    return response.data
  },

  logout: async (refreshToken: string): Promise<void> => {
    await apiClient.post('/api/auth/logout', { refreshToken } as LogoutRequest)
  },

  refresh: async (refreshToken: string): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/refresh', {
      refreshToken,
    } as RefreshRequest)
    return response.data
  },

  getProfile: async (): Promise<UserProfileResponse> => {
    const response = await apiClient.get<UserProfileResponse>('/api/users/me')
    return response.data
  },

  updateProfile: async (displayName: string): Promise<UserProfileResponse> => {
    const response = await apiClient.put<UserProfileResponse>('/api/users/me', {
      displayName,
    })
    return response.data
  },

  getUserProfile: async (userId: number): Promise<UserProfileResponse> => {
    const response = await apiClient.get<UserProfileResponse>(`/api/users/${userId}`)
    return response.data
  },
}