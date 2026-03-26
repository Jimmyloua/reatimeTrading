import { NotificationItem } from './NotificationItem'
import { Skeleton } from '@/components/ui/skeleton'
import type { Notification } from '@/types/notification'

interface NotificationListProps {
  notifications: Notification[]
  isLoading?: boolean
  onMarkAsRead: (id: number) => Promise<void> | void
}

export function NotificationList({
  notifications,
  isLoading = false,
  onMarkAsRead,
}: NotificationListProps) {

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
    <div className="divide-y">
      {notifications.map((notification) => (
          <NotificationItem
            key={notification.id}
            notification={notification}
            onMarkAsRead={onMarkAsRead}
          />
      ))}
    </div>
  )
}
