import { formatDistanceToNow } from 'date-fns'
import { Bell, MessageSquare, Package, AlertCircle, DollarSign } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { Notification } from '@/types/notification'

interface NotificationItemProps {
  notification: Notification
  onMarkAsRead: (id: number) => void
  onClick?: () => void
}

const iconMap = {
  NEW_MESSAGE: MessageSquare,
  ITEM_SOLD: Package,
  TRANSACTION_UPDATE: DollarSign,
  SYSTEM_ANNOUNCEMENT: AlertCircle,
  PAYMENT_STATUS: DollarSign
}

export function NotificationItem({ notification, onMarkAsRead, onClick }: NotificationItemProps) {
  const Icon = iconMap[notification.type] || Bell

  return (
    <div
      onClick={onClick}
      className={cn(
        'flex items-start gap-3 p-3 cursor-pointer hover:bg-neutral-50 transition-colors',
        !notification.read && 'bg-blue-50'
      )}
    >
      <div className="mt-1">
        <Icon className="h-5 w-5 text-neutral-500" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-medium text-sm">{notification.title}</p>
        <p className="text-sm text-neutral-600 mt-0.5">{notification.content}</p>
        <p className="text-xs text-neutral-400 mt-1">
          {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
        </p>
      </div>
      {!notification.read && (
        <button
          onClick={(e) => {
            e.stopPropagation()
            onMarkAsRead(notification.id)
          }}
          className="text-xs text-blue-600 hover:underline whitespace-nowrap"
        >
          Mark as read
        </button>
      )}
    </div>
  )
}