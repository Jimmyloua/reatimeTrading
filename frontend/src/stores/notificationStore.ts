import { create } from 'zustand'
import type { Notification, NotificationPreferences } from '@/types/notification'

interface NotificationState {
  notifications: Notification[]
  preferences: NotificationPreferences
  preferencesLoaded: boolean
  unreadCount: number
  isLoading: boolean
  error: string | null

  setNotifications: (notifications: Notification[]) => void
  addNotification: (notification: Notification) => void
  markAsRead: (notificationId: number) => void
  markAllAsRead: () => void
  setPreferences: (preferences: NotificationPreferences) => void
  updatePreference: (key: keyof NotificationPreferences, value: boolean) => void
  setUnreadCount: (count: number) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
}

const initialState = {
  notifications: [],
  preferences: {
    newMessageEnabled: true,
    itemSoldEnabled: true,
    transactionUpdateEnabled: true,
  },
  preferencesLoaded: false,
  unreadCount: 0,
  isLoading: false,
  error: null
}

export const useNotificationStore = create<NotificationState>((set) => ({
  ...initialState,

  setNotifications: (notifications) => set({
    notifications,
    unreadCount: notifications.filter((notification) => !notification.read).length,
  }),

  addNotification: (notification) => set((state) => {
    // Avoid duplicates
    if (state.notifications.some(n => n.id === notification.id)) {
      return state
    }
    return {
      notifications: [notification, ...state.notifications],
      unreadCount: state.unreadCount + (notification.read ? 0 : 1)
    }
  }),

  markAsRead: (notificationId) => set((state) => ({
    notifications: state.notifications.map(n =>
      n.id === notificationId ? { ...n, read: true, readAt: new Date().toISOString() } : n
    ),
    unreadCount: Math.max(
      0,
      state.unreadCount - (state.notifications.some((n) => n.id === notificationId && !n.read) ? 1 : 0)
    )
  })),

  markAllAsRead: () => set((state) => ({
    notifications: state.notifications.map(n => ({ ...n, read: true })),
    unreadCount: 0
  })),

  setPreferences: (preferences) => set({
    preferences,
    preferencesLoaded: true,
  }),

  updatePreference: (key, value) => set((state) => ({
    preferences: {
      ...state.preferences,
      [key]: value,
    },
    preferencesLoaded: true,
  })),

  setUnreadCount: (count) => set({ unreadCount: count }),

  setLoading: (loading) => set({ isLoading: loading }),

  setError: (error) => set({ error })
}))
