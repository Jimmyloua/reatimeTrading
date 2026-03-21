import { create } from 'zustand'
import type { Message, Conversation } from '@/types/chat'

interface ChatState {
  conversations: Conversation[]
  activeConversation: Conversation | null
  messages: Message[]
  typingUsers: Map<number, boolean>
  isLoading: boolean
  error: string | null

  setConversations: (conversations: Conversation[]) => void
  addConversation: (conversation: Conversation) => void
  setActiveConversation: (conversation: Conversation | null) => void
  setMessages: (messages: Message[]) => void
  addMessage: (message: Message) => void
  prependMessages: (messages: Message[]) => void
  setTyping: (userId: number, isTyping: boolean) => void
  incrementUnread: (conversationId: number) => void
  clearUnread: (conversationId: number) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
  reset: () => void
}

const initialState = {
  conversations: [],
  activeConversation: null,
  messages: [],
  typingUsers: new Map(),
  isLoading: false,
  error: null
}

export const useChatStore = create<ChatState>((set) => ({
  ...initialState,

  setConversations: (conversations) => set({ conversations }),

  addConversation: (conversation) => set((state) => ({
    conversations: [conversation, ...state.conversations]
  })),

  setActiveConversation: (conversation) => set({
    activeConversation: conversation,
    messages: [],
    typingUsers: new Map()
  }),

  setMessages: (messages) => set({ messages }),

  addMessage: (message) => set((state) => ({
    messages: [...state.messages, message]
  })),

  prependMessages: (messages) => set((state) => ({
    messages: [...messages, ...state.messages]
  })),

  setTyping: (userId, isTyping) => set((state) => {
    const newTyping = new Map(state.typingUsers)
    if (isTyping) {
      newTyping.set(userId, isTyping)
    } else {
      newTyping.delete(userId)
    }
    return { typingUsers: newTyping }
  }),

  incrementUnread: (conversationId) => set((state) => ({
    conversations: state.conversations.map(c =>
      c.id === conversationId
        ? { ...c, unreadCount: (c.unreadCount || 0) + 1 }
        : c
    )
  })),

  clearUnread: (conversationId) => set((state) => ({
    conversations: state.conversations.map(c =>
      c.id === conversationId ? { ...c, unreadCount: 0 } : c
    )
  })),

  setLoading: (loading) => set({ isLoading: loading }),

  setError: (error) => set({ error }),

  reset: () => set(initialState)
}))