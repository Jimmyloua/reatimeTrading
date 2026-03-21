import { useEffect } from 'react'
import { useNotificationStore } from '@/stores/notificationStore'
import { useWebSocket } from './useWebSocket'
import { notificationApi } from '@/api/notificationApi'
import type { Notification } from '@/types/notification'

export function useNotifications() {
  const { subscribe, connectionState } = useWebSocket()
  const { addNotification, setUnreadCount } = useNotificationStore()

  // Subscribe to real-time notifications
  useEffect(() => {
    if (connectionState !== 'connected') return

    const subscription = subscribe(
      '/user/queue/notifications',
      (message) => {
        const notification: Notification = JSON.parse(message.body)
        addNotification(notification)
      }
    )

    return () => {
      subscription?.unsubscribe()
    }
  }, [connectionState, subscribe, addNotification])

  // Load initial unread count
  useEffect(() => {
    const loadUnreadCount = async () => {
      try {
        const count = await notificationApi.getUnreadCount()
        setUnreadCount(count)
      } catch (error) {
        console.error('Failed to load unread count:', error)
      }
    }

    loadUnreadCount()
  }, [setUnreadCount])

  return {
    connectionState
  }
}