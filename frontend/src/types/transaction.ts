// Transaction types matching backend DTOs

export type TransactionStatus =
  | 'CREATED'
  | 'FUNDED'
  | 'RESERVED'
  | 'DELIVERED'
  | 'CONFIRMED'
  | 'SETTLED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'DISPUTED'
  | 'REFUNDED'

export interface Transaction {
  id: number
  listingId: number
  listingTitle: string
  listingImageUrl: string | null
  buyerId: number
  buyerName: string
  sellerId: number
  sellerName: string
  amount: string
  status: TransactionStatus
  createdAt: string
  updatedAt: string
  userRole: 'BUYER' | 'SELLER'
}

export interface TransactionDetail extends Transaction {
  listingDescription: string
  buyerAvatarUrl: string | null
  sellerAvatarUrl: string | null

  // Lifecycle timestamps for timeline
  fundedAt: string | null
  reservedAt: string | null
  deliveredAt: string | null
  confirmedAt: string | null
  settledAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  expiredAt: string | null
  cancellationReason: string | null

  // Available actions
  canCancel: boolean
  canConfirmPayment: boolean
  canConfirmFunds: boolean
  canMarkDelivered: boolean
  canConfirmReceipt: boolean
  canRate: boolean
  canDispute: boolean
}

export interface TransactionRequest {
  listingId: number
  conversationId?: number
  idempotencyKey: string
}

export interface TransactionActionRequest {
  idempotencyKey: string
  cancellationReason?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// Status color mapping per UI-SPEC
export const TRANSACTION_STATUS_COLORS: Record<TransactionStatus, string> = {
  CREATED: 'bg-zinc-500',
  FUNDED: 'bg-green-500',
  RESERVED: 'bg-teal-400',
  DELIVERED: 'bg-cyan-400',
  CONFIRMED: 'bg-emerald-400',
  SETTLED: 'bg-green-500',
  COMPLETED: 'bg-neutral-800',
  CANCELLED: 'bg-zinc-500',
  EXPIRED: 'bg-zinc-400',
  DISPUTED: 'bg-amber-500',
  REFUNDED: 'bg-orange-500',
}

// Status display names
export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  CREATED: 'Pending',
  FUNDED: 'Payment Sent',
  RESERVED: 'Funds Received',
  DELIVERED: 'Shipped',
  CONFIRMED: 'Delivered',
  SETTLED: 'Settled',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  EXPIRED: 'Expired',
  DISPUTED: 'Under Dispute',
  REFUNDED: 'Refunded',
}