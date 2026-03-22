export interface Notification {
  id: number
  type: 'NEW_MESSAGE' | 'ITEM_SOLD' | 'TRANSACTION_UPDATE' | 'SYSTEM_ANNOUNCEMENT' | 'PAYMENT_STATUS'
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
