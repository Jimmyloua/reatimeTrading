import apiClient from './client'
import type { Notification } from '@/types/notification'

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

  async markAsRead(notificationId: number): Promise<void> {
    await apiClient.patch(`/api/notifications/${notificationId}/read`)
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.patch('/api/notifications/read-all')
  }
}