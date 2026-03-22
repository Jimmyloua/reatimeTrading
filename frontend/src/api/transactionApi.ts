// Transaction API client
// Note: This file will be extended by plan 04-04 with full transaction support

import apiClient from './client'
import type { Transaction, TransactionDetail, TransactionRequest } from '@/types/transaction'
import type { PaginatedResponse } from '@/types/listing'

const API_BASE = '/api/transactions'

// Generate a random idempotency key
const generateIdempotencyKey = (): string => {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`
}

export const transactionApi = {
  /**
   * Create a new transaction (Request to Buy)
   */
  async createTransaction(data: TransactionRequest): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(API_BASE, data)
    return response.data
  },

  /**
   * Get paginated purchases for current user
   */
  async getPurchases(page = 0, size = 20): Promise<PaginatedResponse<Transaction>> {
    const response = await apiClient.get<PaginatedResponse<Transaction>>(`${API_BASE}/purchases`, {
      params: { page, size },
    })
    return response.data
  },

  /**
   * Get paginated sales for current user
   */
  async getSales(page = 0, size = 20): Promise<PaginatedResponse<Transaction>> {
    const response = await apiClient.get<PaginatedResponse<Transaction>>(`${API_BASE}/sales`, {
      params: { page, size },
    })
    return response.data
  },

  /**
   * Get transaction detail
   */
  async getTransaction(id: number): Promise<TransactionDetail> {
    const response = await apiClient.get<TransactionDetail>(`${API_BASE}/${id}`)
    return response.data
  },

  /**
   * Accept a purchase request (seller)
   */
  async acceptRequest(id: number): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(`${API_BASE}/${id}/accept`, {
      idempotencyKey: generateIdempotencyKey(),
    })
    return response.data
  },

  /**
   * Decline a purchase request (seller)
   */
  async declineRequest(id: number, reason?: string): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(`${API_BASE}/${id}/decline`, {
      idempotencyKey: generateIdempotencyKey(),
      cancellationReason: reason,
    })
    return response.data
  },

  /**
   * Confirm payment sent (buyer)
   */
  async confirmPayment(id: number): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(`${API_BASE}/${id}/confirm-payment`, {
      idempotencyKey: generateIdempotencyKey(),
    })
    return response.data
  },

  /**
   * Confirm funds received (seller)
   */
  async confirmFunds(id: number): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(`${API_BASE}/${id}/confirm-funds`, {
      idempotencyKey: generateIdempotencyKey(),
    })
    return response.data
  },

  /**
   * Mark as delivered (seller)
   */
  async markDelivered(id: number): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(`${API_BASE}/${id}/mark-delivered`, {
      idempotencyKey: generateIdempotencyKey(),
    })
    return response.data
  },

  /**
   * Confirm receipt (buyer)
   */
  async confirmReceipt(id: number): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(`${API_BASE}/${id}/confirm-receipt`, {
      idempotencyKey: generateIdempotencyKey(),
    })
    return response.data
  },

  /**
   * Cancel transaction
   */
  async cancelTransaction(id: number, reason?: string): Promise<Transaction> {
    const response = await apiClient.post<Transaction>(`${API_BASE}/${id}/cancel`, {
      idempotencyKey: generateIdempotencyKey(),
      cancellationReason: reason,
    })
    return response.data
  },
}
