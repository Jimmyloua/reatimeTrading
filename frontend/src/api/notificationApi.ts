import apiClient from './client'
import type { Notification, NotificationPreferences } from '@/types/notification'

export type NotificationManagementTab = 'all' | 'unread'

export interface NotificationQueryParams {
  tab?: NotificationManagementTab
  types?: Notification['type'][]
  page?: number
  size?: number
}

export interface MarkVisibleAsReadParams {
  tab: NotificationManagementTab
  types: Notification['type'][]
  page: number
}

function normalizeNotificationQueryParams(
  paramsOrPage: NotificationQueryParams | number = 0,
  size = 20
): NotificationQueryParams {
  if (typeof paramsOrPage === 'number') {
    return {
      page: paramsOrPage,
      size,
    }
  }

  return paramsOrPage
}

function buildNotificationParams({
  tab,
  types,
  page,
  size,
}: NotificationQueryParams) {
  return {
    ...(tab ? { tab } : {}),
    ...(types?.length ? { types: types.join(',') } : {}),
    ...(page !== undefined ? { page } : {}),
    ...(size !== undefined ? { size } : {}),
  }
}

export const notificationApi = {
  async getNotifications(
    paramsOrPage: NotificationQueryParams | number = 0,
    size = 20
  ): Promise<{ content: Notification[]; totalElements: number }> {
    const params = buildNotificationParams(normalizeNotificationQueryParams(paramsOrPage, size))
    const response = await apiClient.get('/api/notifications', {
      params,
    })
    return response.data
  },

  async getUnreadNotifications(): Promise<Notification[]> {
    const response = await apiClient.get('/api/notifications/unread')
    return response.data
  },

  async getUnreadCount(): Promise<number> {
    const response = await apiClient.get<{ unreadCount: number }>('/api/notifications/count')
    return response.data.unreadCount
  },

  async getPreferences(): Promise<NotificationPreferences> {
    const response = await apiClient.get<NotificationPreferences>('/api/notifications/preferences')
    return response.data
  },

  async updatePreferences(preferences: NotificationPreferences): Promise<NotificationPreferences> {
    const response = await apiClient.patch<NotificationPreferences>('/api/notifications/preferences', preferences)
    return response.data
  },

  async markAsRead(notificationId: number): Promise<Notification> {
    const response = await apiClient.patch<Notification>(`/api/notifications/${notificationId}/read`)
    return response.data
  },

  async markVisibleAsRead({ tab, types, page }: MarkVisibleAsReadParams): Promise<void> {
    await apiClient.patch('/api/notifications/read-visible', undefined, {
      params: buildNotificationParams({ tab, types, page }),
    })
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.patch('/api/notifications/read-all')
  }
}
