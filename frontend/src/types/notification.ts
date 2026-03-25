export interface Notification {
  id: number
  type: 'NEW_MESSAGE' | 'SELLER_ONLINE' | 'ITEM_SOLD' | 'TRANSACTION_UPDATE' | 'SYSTEM_ANNOUNCEMENT' | 'PAYMENT_STATUS'
  title: string
  content: string
  referenceId: number | null
  referenceType: string | null
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationPreferences {
  newMessageEnabled: boolean
  itemSoldEnabled: boolean
  transactionUpdateEnabled: boolean
}

export function getNotificationPreferenceKey(
  notificationType: Notification['type']
): keyof NotificationPreferences | null {
  switch (notificationType) {
    case 'NEW_MESSAGE':
    case 'SELLER_ONLINE':
      return 'newMessageEnabled'
    case 'ITEM_SOLD':
      return 'itemSoldEnabled'
    case 'TRANSACTION_UPDATE':
    case 'PAYMENT_STATUS':
      return 'transactionUpdateEnabled'
    default:
      return null
  }
}

export function filterNotificationsByPreferences(
  notifications: Notification[],
  preferences: NotificationPreferences
): Notification[] {
  return notifications.filter((notification) => {
    const preferenceKey = getNotificationPreferenceKey(notification.type)

    if (!preferenceKey) {
      return true
    }

    return preferences[preferenceKey]
  })
}
