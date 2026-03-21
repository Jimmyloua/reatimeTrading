import { NotificationList } from '@/components/notifications/NotificationList'
import { useNotifications } from '@/hooks/useNotifications'

export default function NotificationsPage() {
  // Initialize WebSocket subscription
  useNotifications()

  return (
    <div className="container mx-auto py-8">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold mb-6">Notifications</h1>
        <div className="border rounded-lg overflow-hidden bg-white">
          <NotificationList />
        </div>
      </div>
    </div>
  )
}