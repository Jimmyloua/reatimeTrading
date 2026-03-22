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
  upsertConversation: (conversation: Conversation) => void
  syncConversationPreview: (message: Message) => void
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

  setConversations: (conversations) => set((state) => {
    const activeConversation =
      state.activeConversation
        ? conversations.find((conversation) => conversation.id === state.activeConversation?.id) ?? state.activeConversation
        : null

    return {
      conversations,
      activeConversation,
    }
  }),

  addConversation: (conversation) => set((state) => ({
    conversations: state.conversations.some((current) => current.id === conversation.id)
      ? state.conversations
      : [conversation, ...state.conversations]
  })),

  upsertConversation: (conversation) => set((state) => {
    const withoutConversation = state.conversations.filter((current) => current.id !== conversation.id)

    return {
      conversations: [conversation, ...withoutConversation],
      activeConversation:
        state.activeConversation?.id === conversation.id ? conversation : state.activeConversation,
    }
  }),

  syncConversationPreview: (message) => set((state) => {
    const conversation = state.conversations.find((current) => current.id === message.conversationId)
    if (!conversation) {
      return state
    }

    const updatedConversation: Conversation = {
      ...conversation,
      lastMessage: message.content,
      lastMessageAt: message.createdAt,
    }

    return {
      conversations: [
        updatedConversation,
        ...state.conversations.filter((current) => current.id !== message.conversationId),
      ],
      activeConversation:
        state.activeConversation?.id === message.conversationId
          ? updatedConversation
          : state.activeConversation,
    }
  }),

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
    ),
    activeConversation:
      state.activeConversation?.id === conversationId
        ? {
            ...state.activeConversation,
            unreadCount: (state.activeConversation.unreadCount || 0) + 1,
          }
        : state.activeConversation,
  })),

  clearUnread: (conversationId) => set((state) => ({
    conversations: state.conversations.map(c =>
      c.id === conversationId ? { ...c, unreadCount: 0 } : c
    ),
    activeConversation:
      state.activeConversation?.id === conversationId
        ? {
            ...state.activeConversation,
            unreadCount: 0,
          }
        : state.activeConversation,
  })),

  setLoading: (loading) => set({ isLoading: loading }),

  setError: (error) => set({ error }),

  reset: () => set(initialState)
}))
