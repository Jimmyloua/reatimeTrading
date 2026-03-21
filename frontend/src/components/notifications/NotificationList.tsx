import { useNotificationStore } from '@/stores/notificationStore'
import { notificationApi } from '@/api/notificationApi'
import { NotificationItem } from './NotificationItem'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { useEffect } from 'react'

export function NotificationList() {
  const { notifications, isLoading, setNotifications, markAsRead, markAllAsRead, setLoading } = useNotificationStore()

  useEffect(() => {
    const fetchNotifications = async () => {
      setLoading(true)
      try {
        const response = await notificationApi.getNotifications()
        setNotifications(response.content)
      } catch (error) {
        console.error('Failed to fetch notifications:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchNotifications()
  }, [setNotifications, setLoading])

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationApi.markAsRead(id)
      markAsRead(id)
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead()
      markAllAsRead()
    } catch (error) {
      console.error('Failed to mark all as read:', error)
    }
  }

  if (isLoading) {
    return (
      <div className="p-4 space-y-3">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="flex gap-3">
            <Skeleton className="h-10 w-10 rounded-full" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-3 w-full" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (notifications.length === 0) {
    return (
      <div className="p-4 text-center">
        <p className="text-neutral-500">No notifications</p>
        <p className="text-sm text-neutral-400 mt-1">
          When you have new messages or activity on your items, you'll see it here.
        </p>
      </div>
    )
  }

  return (
    <div>
      {notifications.some(n => !n.read) && (
        <div className="p-2 border-b flex justify-end">
          <Button variant="ghost" size="sm" onClick={handleMarkAllAsRead}>
            Mark all as read
          </Button>
        </div>
      )}
      <div className="divide-y">
        {notifications.map((notification) => (
          <NotificationItem
            key={notification.id}
            notification={notification}
            onMarkAsRead={handleMarkAsRead}
          />
        ))}
      </div>
    </div>
  )
}