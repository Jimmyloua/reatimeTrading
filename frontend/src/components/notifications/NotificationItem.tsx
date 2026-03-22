import { formatDistanceToNow } from 'date-fns'
import { Bell, MessageSquare, Package, AlertCircle, DollarSign } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
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

function resolveNotificationPath(notification: Notification): string | null {
  if (!notification.referenceId) {
    return null
  }

  const referenceType = notification.referenceType?.toLowerCase()

  if (
    notification.type === 'NEW_MESSAGE' &&
    (!referenceType || referenceType === 'conversation')
  ) {
    return `/messages?conversation=${notification.referenceId}`
  }

  if (notification.type === 'ITEM_SOLD' && referenceType === 'listing') {
    return `/listings/${notification.referenceId}`
  }

  if (
    (notification.type === 'TRANSACTION_UPDATE' || notification.type === 'PAYMENT_STATUS') &&
    referenceType === 'transaction'
  ) {
    return `/transactions/${notification.referenceId}`
  }

  return null
}

export function NotificationItem({ notification, onMarkAsRead, onClick }: NotificationItemProps) {
  const Icon = iconMap[notification.type] || Bell
  const navigate = useNavigate()

  const handleOpen = async () => {
    if (!notification.read) {
      await onMarkAsRead(notification.id)
    }

    onClick?.()

    const destination = resolveNotificationPath(notification)
    if (destination) {
      navigate(destination)
    }
  }

  return (
    <div
      onClick={() => {
        void handleOpen()
      }}
      role="button"
      tabIndex={0}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          void handleOpen()
        }
      }}
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
