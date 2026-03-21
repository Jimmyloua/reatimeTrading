export interface User {
  id: number
  email: string
  displayName: string | null
  avatarUrl: string | null
  createdAt: string
  listingCount: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  expiresIn: number
}

export interface RegisterRequest {
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

export interface UserProfileResponse {
  id: number
  email: string
  displayName: string | null
  avatarUrl: string | null
  createdAt: string
  listingCount: number
}

export interface UpdateProfileRequest {
  displayName: string
}