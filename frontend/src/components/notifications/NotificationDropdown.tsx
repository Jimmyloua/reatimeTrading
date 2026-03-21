import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useNotificationStore } from '@/stores/notificationStore'
import { useNotifications } from '@/hooks/useNotifications'
import { NotificationItem } from './NotificationItem'
import { notificationApi } from '@/api/notificationApi'

export function NotificationDropdown() {
  const { notifications, unreadCount, markAsRead } = useNotificationStore()
  useNotifications()

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationApi.markAsRead(id)
      markAsRead(id)
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  const recentNotifications = notifications.slice(0, 5)

  return (
    <div>
      <div className="p-3 border-b">
        <h3 className="font-semibold">Notifications</h3>
        {unreadCount > 0 && (
          <p className="text-sm text-neutral-500">{unreadCount} unread</p>
        )}
      </div>

      {recentNotifications.length === 0 ? (
        <div className="p-4 text-center text-neutral-500">
          <p>No new notifications</p>
        </div>
      ) : (
        <div className="max-h-80 overflow-y-auto">
          {recentNotifications.map((notification) => (
            <NotificationItem
              key={notification.id}
              notification={notification}
              onMarkAsRead={handleMarkAsRead}
            />
          ))}
        </div>
      )}

      <div className="p-2 border-t">
        <Link to="/notifications">
          <Button variant="ghost" className="w-full">
            View all notifications
          </Button>
        </Link>
      </div>
    </div>
  )
}