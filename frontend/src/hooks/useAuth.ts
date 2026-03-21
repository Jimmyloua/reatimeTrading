import { useAuthStore } from '@/stores/authStore'
import type { User } from '@/types/user'

export function useAuth() {
  const { user, isAuthenticated, accessToken, refreshToken, setTokens, setUser, logout, clearTokens } = useAuthStore()

  const login = async (email: string, accessToken: string, refreshToken: string, userId: number) => {
    setTokens(accessToken, refreshToken)
    setUser({
      id: userId,
      email,
      displayName: null,
      avatarUrl: null,
      createdAt: new Date().toISOString(),
      listingCount: 0,
    } as User)
  }

  const register = async (email: string, accessToken: string, refreshToken: string, userId: number) => {
    setTokens(accessToken, refreshToken)
    setUser({
      id: userId,
      email,
      displayName: null,
      avatarUrl: null,
      createdAt: new Date().toISOString(),
      listingCount: 0,
    } as User)
  }

  const refreshUser = async (userData: User) => {
    setUser(userData)
  }

  return {
    user,
    isAuthenticated,
    accessToken,
    refreshToken,
    login,
    register,
    logout,
    clearTokens,
    refreshUser,
  }
}