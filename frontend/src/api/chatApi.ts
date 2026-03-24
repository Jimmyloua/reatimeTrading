import apiClient from './client'
import type { Conversation, Message } from '@/types/chat'

export interface CreateConversationRequest {
  listingId: number
  initialMessage?: string
}

export interface SendMessageRequest {
  conversationId: number
  content?: string
  imageUrl?: string
}

export const chatApi = {
  async createConversation(request: CreateConversationRequest): Promise<Conversation> {
    const response = await apiClient.post<Conversation>('/api/conversations', request)
    return response.data
  },

  async getConversations(page = 0, size = 20): Promise<{ content: Conversation[]; totalElements: number }> {
    const response = await apiClient.get('/api/conversations', {
      params: { page, size }
    })
    return response.data
  },

  async getConversation(id: number): Promise<Conversation> {
    const response = await apiClient.get<Conversation>(`/api/conversations/${id}`)
    return response.data
  },

  async getMessages(conversationId: number, page = 0, size = 50): Promise<{ content: Message[]; totalElements: number }> {
    const response = await apiClient.get(`/api/conversations/${conversationId}/messages`, {
      params: { page, size }
    })
    return response.data
  },

  async sendMessage(conversationId: number, request: Omit<SendMessageRequest, 'conversationId'>): Promise<Message> {
    const response = await apiClient.post<Message>(`/api/conversations/${conversationId}/messages`, {
      conversationId,
      ...request,
    })
    return response.data
  }
}
