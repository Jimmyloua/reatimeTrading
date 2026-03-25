import apiClient from './client'
import type { Notification, NotificationPreferences } from '@/types/notification'

export const notificationApi = {
  async getNotifications(page = 0, size = 20): Promise<{ content: Notification[]; totalElements: number }> {
    const response = await apiClient.get('/api/notifications', {
      params: { page, size }
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

  async markAllAsRead(): Promise<void> {
    await apiClient.patch('/api/notifications/read-all')
  }
}
