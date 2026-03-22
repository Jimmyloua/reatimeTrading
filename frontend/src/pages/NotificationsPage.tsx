import { useEffect } from 'react'
import { NotificationList } from '@/components/notifications/NotificationList'
import { useNotifications } from '@/hooks/useNotifications'
import { notificationApi } from '@/api/notificationApi'
import { useNotificationStore } from '@/stores/notificationStore'

const defaultPreferences = {
  newMessageEnabled: true,
  itemSoldEnabled: true,
  transactionUpdateEnabled: true,
}

export default function NotificationsPage() {
  // Initialize WebSocket subscription
  useNotifications()
  const store = useNotificationStore()
  const preferences = store.preferences ?? defaultPreferences
  const preferencesLoaded = store.preferencesLoaded ?? false
  const setPreferences = store.setPreferences
  const updatePreference = store.updatePreference

  useEffect(() => {
    const hydratePreferences = async () => {
      if (preferencesLoaded) {
        return
      }

      try {
        const persistedPreferences = await notificationApi.getPreferences()
        setPreferences(persistedPreferences)
      } catch (error) {
        console.error('Failed to load notification preferences:', error)
      }
    }

    void hydratePreferences()
  }, [preferencesLoaded, setPreferences])

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
    <div className="container mx-auto py-8">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold mb-6">Notifications</h1>
        <div className="mb-4 rounded-lg border bg-white p-4">
          <h2 className="text-sm font-semibold text-neutral-700">Notification preferences</h2>
          <div className="mt-3 space-y-3">
            <label className="flex items-center justify-between gap-3 text-sm">
              <span>New messages</span>
              <input
                type="checkbox"
                aria-label="New messages"
                checked={preferences.newMessageEnabled}
                onChange={(event) => {
                  void handlePreferenceChange('newMessageEnabled', event.target.checked)
                }}
              />
            </label>
            <label className="flex items-center justify-between gap-3 text-sm">
              <span>Item sold</span>
              <input
                type="checkbox"
                aria-label="Item sold"
                checked={preferences.itemSoldEnabled}
                onChange={(event) => {
                  void handlePreferenceChange('itemSoldEnabled', event.target.checked)
                }}
              />
            </label>
            <label className="flex items-center justify-between gap-3 text-sm">
              <span>Transaction updates</span>
              <input
                type="checkbox"
                aria-label="Transaction updates"
                checked={preferences.transactionUpdateEnabled}
                onChange={(event) => {
                  void handlePreferenceChange('transactionUpdateEnabled', event.target.checked)
                }}
              />
            </label>
          </div>
        </div>
        <div className="border rounded-lg overflow-hidden bg-white">
          <NotificationList />
        </div>
      </div>
    </div>
  )
}
