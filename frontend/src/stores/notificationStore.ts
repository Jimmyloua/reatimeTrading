import { create } from 'zustand'
import type { Notification, NotificationPreferences } from '@/types/notification'

interface NotificationState {
  notifications: Notification[]
  preferences: NotificationPreferences
  preferencesLoaded: boolean
  unreadCount: number
  unreadCountHydrated: boolean
  isLoading: boolean
  error: string | null

  setNotifications: (notifications: Notification[]) => void
  addNotification: (notification: Notification) => void
  upsertNotification: (notification: Notification) => void
  markAsRead: (notification: Notification | number) => void
  markVisibleAsRead: (notificationIds: number[]) => void
  markAllAsRead: () => void
  setPreferences: (preferences: NotificationPreferences) => void
  updatePreference: (key: keyof NotificationPreferences, value: boolean) => void
  setUnreadCount: (count: number) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
}

function sortNotifications(notifications: Notification[]): Notification[] {
  return [...notifications].sort((left, right) => {
    const createdAtDelta = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()

    if (createdAtDelta !== 0) {
      return createdAtDelta
    }

    return right.id - left.id
  })
}

function mergeNotifications(current: Notification[], incoming: Notification[]): Notification[] {
  const merged = new Map<number, Notification>()

  for (const notification of current) {
    merged.set(notification.id, notification)
  }

  for (const notification of incoming) {
    merged.set(notification.id, notification)
  }

  return sortNotifications(Array.from(merged.values()))
}

function countUnread(notifications: Notification[]): number {
  return notifications.filter((notification) => !notification.read).length
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
  unreadCountHydrated: false,
  isLoading: false,
  error: null
}

export const useNotificationStore = create<NotificationState>((set) => ({
  ...initialState,

  setNotifications: (notifications) => set((state) => {
    const mergedNotifications = mergeNotifications(state.notifications, notifications)

    return {
      notifications: mergedNotifications,
      unreadCount: state.unreadCountHydrated ? state.unreadCount : countUnread(mergedNotifications),
    }
  }),

  addNotification: (notification) => set((state) => {
    const existingNotification = state.notifications.find((current) => current.id === notification.id)
    const mergedNotifications = mergeNotifications(state.notifications, [notification])
    const unreadDelta = !existingNotification
      ? notification.read
        ? 0
        : 1
      : Number(!notification.read) - Number(!existingNotification.read)

    return {
      notifications: mergedNotifications,
      unreadCount: Math.max(0, state.unreadCount + unreadDelta),
    }
  }),

  upsertNotification: (notification) => set((state) => {
    const existingNotification = state.notifications.find((current) => current.id === notification.id)
    const mergedNotifications = mergeNotifications(state.notifications, [notification])
    const unreadDelta = !existingNotification
      ? notification.read
        ? 0
        : 1
      : Number(!notification.read) - Number(!existingNotification.read)

    return {
      notifications: mergedNotifications,
      unreadCount: Math.max(0, state.unreadCount + unreadDelta),
    }
  }),

  markAsRead: (notificationOrId) => set((state) => {
    const notificationId = typeof notificationOrId === 'number' ? notificationOrId : notificationOrId.id
    const updatedNotification = typeof notificationOrId === 'number' ? null : notificationOrId
    const wasUnread = state.notifications.some((notification) => notification.id === notificationId && !notification.read)

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
      unreadCount: Math.max(0, state.unreadCount - Number(wasUnread))
    }
  }),

  markVisibleAsRead: (notificationIds) => set((state) => {
    if (notificationIds.length === 0) {
      return state
    }

    const notificationIdSet = new Set(notificationIds)
    let markedUnreadCount = 0

    const notifications = state.notifications.map((notification) => {
      if (!notificationIdSet.has(notification.id) || notification.read) {
        return notification
      }

      markedUnreadCount += 1

      return {
        ...notification,
        read: true,
        readAt: notification.readAt ?? new Date().toISOString(),
      }
    })

    if (markedUnreadCount === 0) {
      return state
    }

    return {
      notifications,
      unreadCount: Math.max(0, state.unreadCount - markedUnreadCount),
    }
  }),

  markAllAsRead: () => set((state) => ({
    notifications: state.notifications.map((notification) => ({
      ...notification,
      read: true,
      readAt: notification.readAt ?? new Date().toISOString(),
    })),
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

  setUnreadCount: (count) => set({ unreadCount: count, unreadCountHydrated: true }),

  setLoading: (loading) => set({ isLoading: loading }),

  setError: (error) => set({ error })
}))
