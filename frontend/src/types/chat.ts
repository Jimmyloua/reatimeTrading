export interface Message {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  content: string
  imageUrl: string | null
  status: 'SENT' | 'PERSISTED' | 'DELIVERED' | 'READ'
  createdAt: string
  isOwnMessage: boolean
  clientMessageId?: string
}

export interface MessageAck {
  clientMessageId: string | null
  messageId: number
  conversationId: number
  status: 'PERSISTED' | 'DELIVERED' | 'READ'
  createdAt: string
}

export interface Conversation {
  id: number
  listingId: number
  listingTitle: string
  otherUserId: number
  otherUserName: string
  otherUserAvatar: string | null
  otherUserOnline?: boolean
  otherUserLastSeen?: string
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number
  createdAt: string
}

export interface TypingIndicator {
  userId: number
  username: string
  typing: boolean
}

export interface PresenceUpdate {
  userId: number
  online: boolean
  lastSeenText: string
  updatedAt: string
}
