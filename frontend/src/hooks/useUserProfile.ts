import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userApi } from '@/api/userApi'
import { useAuthStore } from '@/stores/authStore'
import type { UpdateProfileRequest } from '@/types/user'
import { toast } from 'sonner'

/**
 * Hook for fetching and managing the current user's profile
 */
export function useUserProfile() {
  const queryClient = useQueryClient()
  const { setUser } = useAuthStore()

  // Fetch profile data
  const {
    data: profile,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['user', 'profile'],
    queryFn: userApi.getProfile,
    staleTime: 5 * 60 * 1000, // 5 minutes
  })

  // Update profile mutation
  const updateMutation = useMutation({
    mutationFn: (data: UpdateProfileRequest) => userApi.updateProfile(data),
    onSuccess: (updatedUser) => {
      // Update the query cache
      queryClient.setQueryData(['user', 'profile'], updatedUser)
      // Update auth store user
      setUser(updatedUser)
      toast.success('Profile updated successfully')
    },
    onError: (error: Error) => {
      toast.error(`Failed to update profile: ${error.message}`)
    },
  })

  return {
    profile,
    isLoading,
    error,
    updateProfile: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
    refetch,
  }
}

/**
 * Hook for fetching a public user profile by ID
 */
export function usePublicProfile(userId: number | undefined) {
  const {
    data: profile,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['user', 'public', userId],
    queryFn: () => userApi.getUserById(userId!),
    enabled: !!userId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  })

  return {
    profile,
    isLoading,
    error,
  }
}