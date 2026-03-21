export interface Message {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  content: string
  imageUrl: string | null
  status: 'SENT' | 'DELIVERED' | 'READ'
  createdAt: string
  isOwnMessage: boolean
}

export interface Conversation {
  id: number
  listingId: number
  listingTitle: string
  otherUserId: number
  otherUserName: string
  otherUserAvatar: string | null
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