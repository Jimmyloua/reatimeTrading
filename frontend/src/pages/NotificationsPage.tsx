import { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { NotificationList } from '@/components/notifications/NotificationList'
import { useNotifications } from '@/hooks/useNotifications'
import {
  notificationApi,
  type NotificationManagementTab,
} from '@/api/notificationApi'
import { NotificationManagementToolbar } from '@/components/notifications/NotificationManagementToolbar'
import { useNotificationStore } from '@/stores/notificationStore'
import { filterNotificationsByPreferences } from '@/types/notification'

const defaultPreferences = {
  newMessageEnabled: true,
  itemSoldEnabled: true,
  transactionUpdateEnabled: true,
}

const DEFAULT_PAGE_SIZE = 20
const managedNotificationTypes = ['NEW_MESSAGE', 'ITEM_SOLD', 'TRANSACTION_UPDATE'] as const

type ManagedNotificationType = (typeof managedNotificationTypes)[number]

function isManagedNotificationType(value: string): value is ManagedNotificationType {
  return managedNotificationTypes.includes(value as ManagedNotificationType)
}

export default function NotificationsPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  // Initialize WebSocket subscription
  useNotifications()
  const store = useNotificationStore()
  const notifications = store.notifications
  const preferences = store.preferences ?? defaultPreferences
  const preferencesLoaded = store.preferencesLoaded ?? false
  const isLoading = store.isLoading
  const setPreferences = store.setPreferences
  const updatePreference = store.updatePreference
  const setNotifications = store.setNotifications
  const setLoading = store.setLoading
  const markAsRead = store.markAsRead

  const tab: NotificationManagementTab = searchParams.get('tab') === 'unread' ? 'unread' : 'all'
  const pageParam = Number.parseInt(searchParams.get('page') ?? '0', 10)
  const page = Number.isFinite(pageParam) && pageParam >= 0 ? pageParam : 0
  const selectedTypesParam = searchParams.get('types') ?? ''
  const selectedTypes = (() => {
    if (!selectedTypesParam) {
      return [...managedNotificationTypes]
    }

    const parsedTypes = selectedTypesParam.split(',').filter(isManagedNotificationType)
    return parsedTypes.length > 0 ? parsedTypes : [...managedNotificationTypes]
  })()
  const selectedTypesKey = selectedTypes.join(',')

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

  useEffect(() => {
    const fetchNotifications = async () => {
      setLoading(true)

      try {
        const response = await notificationApi.getNotifications({
          tab,
          types: selectedTypes,
          page,
          size: DEFAULT_PAGE_SIZE,
        })
        setNotifications(response.content)
      } catch (error) {
        console.error('Failed to fetch notifications:', error)
      } finally {
        setLoading(false)
      }
    }

    void fetchNotifications()
  }, [page, selectedTypesKey, setLoading, setNotifications, tab])

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

  const updateManagementSearchParams = ({
    nextTab = tab,
    nextTypes = selectedTypes,
    nextPage = page,
  }: {
    nextTab?: NotificationManagementTab
    nextTypes?: ManagedNotificationType[]
    nextPage?: number
  }) => {
    const params = new URLSearchParams(searchParams)

    if (nextTab === 'unread') {
      params.set('tab', 'unread')
    } else {
      params.delete('tab')
    }

    if (nextTypes.length === managedNotificationTypes.length) {
      params.delete('types')
    } else {
      params.set('types', nextTypes.join(','))
    }

    params.set('page', String(Math.max(0, nextPage)))
    setSearchParams(params)
  }

  const handleTabChange = (nextTab: NotificationManagementTab) => {
    updateManagementSearchParams({
      nextTab,
      nextPage: 0,
    })
  }

  const handleTypeToggle = (type: ManagedNotificationType) => {
    const nextTypes = selectedTypes.includes(type)
      ? selectedTypes.filter((currentType) => currentType !== type)
      : [...selectedTypes, type]

    updateManagementSearchParams({
      nextTypes: nextTypes.length > 0 ? nextTypes : [...managedNotificationTypes],
      nextPage: 0,
    })
  }

  const visibleNotifications = filterNotificationsByPreferences(notifications, preferences).filter(
    (notification) =>
      selectedTypes.includes(notification.type as ManagedNotificationType) &&
      (tab === 'all' || !notification.read)
  )

  const unreadVisibleNotifications = visibleNotifications.filter((notification) => !notification.read)

  const handleMarkAsRead = async (id: number) => {
    try {
      const updatedNotification = await notificationApi.markAsRead(id)
      markAsRead(updatedNotification)
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  const handleMarkVisibleAsRead = async () => {
    if (unreadVisibleNotifications.length === 0) {
      return
    }

    try {
      await notificationApi.markVisibleAsRead({
        tab,
        types: selectedTypes,
        page,
      })
    } catch (error) {
      console.error('Failed to mark visible notifications as read:', error)
    }
  }

  return (
    <div className="container mx-auto py-8">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold mb-6">Notifications</h1>
        <NotificationManagementToolbar
          hasUnreadVisible={unreadVisibleNotifications.length > 0}
          onMarkVisibleAsRead={() => {
            void handleMarkVisibleAsRead()
          }}
          onTabChange={handleTabChange}
          onTypeToggle={handleTypeToggle}
          tab={tab}
          types={selectedTypes}
        />
        <div className="mb-4 mt-4 rounded-lg border bg-white p-4">
          <h2 className="text-sm font-semibold text-neutral-700">Notification preferences</h2>
          <div className="mt-4 space-y-4">
            <div className="space-y-3">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                Conversation activity
              </h3>
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
            </div>
            <div className="space-y-3">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                Selling activity
              </h3>
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
        </div>
        <div className="border rounded-lg overflow-hidden bg-white">
          <NotificationList
            isLoading={isLoading}
            notifications={visibleNotifications}
            onMarkAsRead={handleMarkAsRead}
          />
        </div>
      </div>
    </div>
  )
}
