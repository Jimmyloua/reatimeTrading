import { create } from 'zustand'
import type { Message, Conversation, MessageAck } from '@/types/chat'

interface ChatState {
  conversations: Conversation[]
  activeConversation: Conversation | null
  messages: Message[]
  seenMessageIds: Set<number>
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
  appendMessages: (messages: Message[]) => void
  prependMessages: (messages: Message[]) => void
  reconcileMessageAck: (ack: MessageAck) => Message | null
  getLatestPersistedMessageId: (conversationId: number) => number | null
  hasSeenMessage: (messageId: number) => boolean
  markMessageSeen: (messageId: number) => void
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
  seenMessageIds: new Set<number>(),
  typingUsers: new Map(),
  isLoading: false,
  error: null
}

function toTimestamp(value: string | null) {
  if (!value) {
    return Number.NEGATIVE_INFINITY
  }

  const timestamp = Date.parse(value)
  return Number.isNaN(timestamp) ? Number.NEGATIVE_INFINITY : timestamp
}

function sortConversationsByLastMessage(conversations: Conversation[]) {
  return [...conversations].sort((left, right) => toTimestamp(right.lastMessageAt) - toTimestamp(left.lastMessageAt))
}

function dedupeMessages(messages: Message[]) {
  const uniqueMessages = new Map<number, Message>()

  for (const message of messages) {
    if (!uniqueMessages.has(message.id)) {
      uniqueMessages.set(message.id, message)
    }
  }

  return Array.from(uniqueMessages.values())
}

export const useChatStore = create<ChatState>((set, get) => ({
  ...initialState,

  setConversations: (conversations) => set((state) => {
    const activeConversation =
      state.activeConversation
        ? conversations.find((conversation) => conversation.id === state.activeConversation?.id) ?? state.activeConversation
        : null

    return {
      conversations: sortConversationsByLastMessage(conversations),
      activeConversation,
    }
  }),

  addConversation: (conversation) => set((state) => ({
    conversations: state.conversations.some((current) => current.id === conversation.id)
      ? state.conversations
      : sortConversationsByLastMessage([conversation, ...state.conversations])
  })),

  upsertConversation: (conversation) => set((state) => {
    const withoutConversation = state.conversations.filter((current) => current.id !== conversation.id)

    return {
      conversations: sortConversationsByLastMessage([conversation, ...withoutConversation]),
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

    const currentTimestamp = toTimestamp(conversation.lastMessageAt)
    const incomingTimestamp = toTimestamp(message.createdAt)

    if (incomingTimestamp <= currentTimestamp) {
      return {
        conversations: state.conversations.map((current) =>
          current.id === message.conversationId ? updatedConversation : current,
        ),
        activeConversation:
          state.activeConversation?.id === message.conversationId
            ? updatedConversation
            : state.activeConversation,
      }
    }

    return {
      conversations: sortConversationsByLastMessage([
        updatedConversation,
        ...state.conversations.filter((current) => current.id !== message.conversationId),
      ]),
      activeConversation:
        state.activeConversation?.id === message.conversationId
          ? updatedConversation
          : state.activeConversation,
    }
  }),

  setActiveConversation: (conversation) => set((state) => {
    const isSameConversation =
      state.activeConversation?.id !== undefined &&
      conversation?.id !== undefined &&
      state.activeConversation.id === conversation.id

    return {
      activeConversation: conversation,
      messages: isSameConversation ? state.messages : [],
      seenMessageIds: state.seenMessageIds,
      typingUsers: isSameConversation ? state.typingUsers : new Map(),
    }
  }),

  setMessages: (messages) => {
    const dedupedMessages = dedupeMessages(messages)
    const nextSeenMessageIds = new Set(get().seenMessageIds)
    dedupedMessages.forEach((message) => nextSeenMessageIds.add(message.id))

    set({
      messages: dedupedMessages,
      seenMessageIds: nextSeenMessageIds,
    })
  },

  addMessage: (message) => set((state) => {
    if (state.seenMessageIds.has(message.id)) {
      return state
    }

    const nextSeenMessageIds = new Set(state.seenMessageIds)
    nextSeenMessageIds.add(message.id)

    return {
      messages: [...state.messages, message],
      seenMessageIds: nextSeenMessageIds,
    }
  }),

  appendMessages: (messages) => set((state) => {
    const nextMessages = [...state.messages]
    const nextSeenMessageIds = new Set(state.seenMessageIds)

    for (const message of messages) {
      if (nextSeenMessageIds.has(message.id)) {
        continue
      }

      nextMessages.push(message)
      nextSeenMessageIds.add(message.id)
    }

    return {
      messages: dedupeMessages(nextMessages),
      seenMessageIds: nextSeenMessageIds,
    }
  }),

  prependMessages: (messages) => set((state) => {
    const dedupedIncomingMessages = messages.filter((message) => !state.seenMessageIds.has(message.id))
    if (dedupedIncomingMessages.length === 0) {
      return state
    }

    const nextSeenMessageIds = new Set(state.seenMessageIds)
    dedupedIncomingMessages.forEach((message) => nextSeenMessageIds.add(message.id))

    return {
      messages: [...dedupedIncomingMessages, ...state.messages],
      seenMessageIds: nextSeenMessageIds,
    }
  }),

  reconcileMessageAck: (ack) => {
    let reconciledMessage: Message | null = null

    set((state) => {
      const messageIndex = state.messages.findIndex((message) =>
        message.clientMessageId === ack.clientMessageId && message.conversationId === ack.conversationId,
      )

      if (messageIndex === -1) {
        return state
      }

      const optimisticMessage = state.messages[messageIndex]
      reconciledMessage = {
        ...optimisticMessage,
        id: ack.messageId,
        status: ack.status,
        createdAt: ack.createdAt,
        clientMessageId: ack.clientMessageId ?? undefined,
      }

      const nextSeenMessageIds = new Set(state.seenMessageIds)
      nextSeenMessageIds.delete(optimisticMessage.id)
      nextSeenMessageIds.add(ack.messageId)

      return {
        messages: dedupeMessages(
          state.messages.map((message, index) => (index === messageIndex ? reconciledMessage : message)),
        ),
        seenMessageIds: nextSeenMessageIds,
      }
    })

    return reconciledMessage
  },

  getLatestPersistedMessageId: (conversationId) => {
    const latestMessage = [...get().messages]
      .filter((message) => message.conversationId === conversationId && message.id > 0)
      .sort((left, right) => right.id - left.id)[0]

    return latestMessage?.id ?? null
  },

  hasSeenMessage: (messageId) => get().seenMessageIds.has(messageId),

  markMessageSeen: (messageId) => set((state) => {
    if (state.seenMessageIds.has(messageId)) {
      return state
    }

    const nextSeenMessageIds = new Set(state.seenMessageIds)
    nextSeenMessageIds.add(messageId)

    return {
      seenMessageIds: nextSeenMessageIds,
    }
  }),

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
