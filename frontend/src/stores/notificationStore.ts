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
  upsertNotification: (notification: Notification) => void
  markAsRead: (notification: Notification | number) => void
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
    const existingIndex = state.notifications.findIndex((current) => current.id === notification.id)
    if (existingIndex >= 0) {
      const nextNotifications = [...state.notifications]
      const previous = nextNotifications[existingIndex]
      nextNotifications[existingIndex] = notification

      return {
        notifications: nextNotifications,
        unreadCount:
          state.unreadCount +
          (previous.read ? 0 : -1) +
          (notification.read ? 0 : 1),
      }
    }
    return {
      notifications: [notification, ...state.notifications],
      unreadCount: state.unreadCount + (notification.read ? 0 : 1)
    }
  }),

  upsertNotification: (notification) => set((state) => {
    const existingIndex = state.notifications.findIndex((current) => current.id === notification.id)
    if (existingIndex === -1) {
      return {
        notifications: [notification, ...state.notifications],
        unreadCount: state.unreadCount + (notification.read ? 0 : 1),
      }
    }

    const previous = state.notifications[existingIndex]
    const nextNotifications = [...state.notifications]
    nextNotifications[existingIndex] = notification

    return {
      notifications: nextNotifications,
      unreadCount:
        state.unreadCount +
        (previous.read ? 0 : -1) +
        (notification.read ? 0 : 1),
    }
  }),

  markAsRead: (notificationOrId) => set((state) => {
    const notificationId = typeof notificationOrId === 'number' ? notificationOrId : notificationOrId.id
    const updatedNotification =
      typeof notificationOrId === 'number'
        ? null
        : notificationOrId

    return {
      notifications: state.notifications.map((notification) => {
        if (notification.id !== notificationId) {
          return notification
        }

        return updatedNotification ?? {
          ...notification,
          read: true,
          readAt: new Date().toISOString(),
        }
      }),
      unreadCount: Math.max(
        0,
        state.unreadCount - (state.notifications.some((n) => n.id === notificationId && !n.read) ? 1 : 0)
      )
    }
  }),

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
