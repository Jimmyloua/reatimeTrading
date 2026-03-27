import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useNotificationStore } from '@/stores/notificationStore'
import { useNotifications } from '@/hooks/useNotifications'
import { NotificationItem } from './NotificationItem'
import { NotificationPreferenceGroups } from './NotificationPreferenceGroups'
import { notificationApi } from '@/api/notificationApi'
import { filterNotificationsByPreferences } from '@/types/notification'

export function NotificationDropdown() {
  const store = useNotificationStore()
  const notifications = store.notifications
  const preferences = store.preferences
  const preferencesLoaded = store.preferencesLoaded
  const unreadCount = store.unreadCount
  const setNotifications = store.setNotifications
  const setPreferences = store.setPreferences
  const markAsRead = store.markAsRead
  const updatePreference = store.updatePreference
  useNotifications()
  const recentNotifications = filterNotificationsByPreferences(
    notifications.filter((notification) => !notification.read),
    preferences
  ).slice(0, 5)

  useEffect(() => {
    const hydrateDropdown = async () => {
      try {
        if (notifications.length === 0) {
          const response = await notificationApi.getNotifications({ page: 0, size: 5 })
          setNotifications(response.content)
        }

        if (!preferencesLoaded) {
          const persistedPreferences = await notificationApi.getPreferences()
          setPreferences(persistedPreferences)
        }
      } catch (error) {
        console.error('Failed to hydrate notification dropdown:', error)
      }
    }

    void hydrateDropdown()
  }, [notifications.length, preferencesLoaded, setNotifications, setPreferences])

  const handleMarkAsRead = async (id: number) => {
    try {
      const updatedNotification = await notificationApi.markAsRead(id)
      markAsRead(updatedNotification)
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  const handlePreferenceChange = async (
    key: keyof typeof preferences,
    value: boolean
  ) => {
    const nextPreferences = {
      ...preferences,
      [key]: value,
    }

    updatePreference(key, value)

    try {
      const persistedPreferences = await notificationApi.updatePreferences(nextPreferences)
      setPreferences(persistedPreferences)
    } catch (error) {
      console.error('Failed to update notification preferences:', error)
      updatePreference(key, preferences[key])
    }
  }

  return (
    <div>
      <div className="p-3 border-b">
        <h3 className="font-semibold">Notifications</h3>
        {unreadCount > 0 && (
          <p className="text-sm text-neutral-500">{unreadCount} unread</p>
        )}
      </div>

      <div className="border-b px-3 py-2 space-y-2">
        <p className="text-xs font-medium uppercase tracking-wide text-neutral-500">
          Quick settings
        </p>
        <NotificationPreferenceGroups
          onPreferenceChange={handlePreferenceChange}
          preferences={preferences}
        />
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
