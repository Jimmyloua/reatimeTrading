import { useCallback, useEffect, useRef } from 'react'
import { chatApi } from '@/api/chatApi'
import { useChatStore } from '@/stores/chatStore'
import { useAuthStore } from '@/stores/authStore'
import { useWebSocket } from './useWebSocket'
import type { Message, TypingIndicator } from '@/types/chat'

const TYPING_DEBOUNCE_MS = 3000

export function useChat(conversationId: number | null) {
  const { subscribe, publish, connectionState } = useWebSocket()
  const currentUser = useAuthStore((state) => state.user)
  const {
    addMessage,
    clearUnread,
    hasSeenMessage,
    incrementUnread,
    markMessageSeen,
    setConversations,
    setMessages,
    setTyping,
    syncConversationPreview,
    upsertConversation,
  } = useChatStore()
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const previousConnectionStateRef = useRef(connectionState)
  const optimisticMessageIdRef = useRef(-1)

  const refreshActiveConversation = useCallback(async () => {
    if (!conversationId) {
      return
    }

    const [latestConversation, latestMessages] = await Promise.all([
      chatApi.getConversation(conversationId),
      chatApi.getMessages(conversationId),
    ])

    upsertConversation(latestConversation)
    setMessages(latestMessages.content.reverse())
    clearUnread(conversationId)
  }, [clearUnread, conversationId, setMessages, upsertConversation])

  const rehydrateConversationState = useCallback(async () => {
    if (!conversationId) {
      return
    }

    const [latestConversation, latestMessages, latestConversations] = await Promise.all([
      chatApi.getConversation(conversationId),
      chatApi.getMessages(conversationId),
      chatApi.getConversations(),
    ])

    setConversations(latestConversations.content)
    upsertConversation(latestConversation)
    setMessages(latestMessages.content.reverse())
    clearUnread(conversationId)
  }, [clearUnread, conversationId, setConversations, setMessages, upsertConversation])

  // Subscribe to message updates for both active and inactive conversations.
  useEffect(() => {
    if (connectionState !== 'connected') return

    const messageSub = subscribe(
      `/user/queue/messages`,
      (message) => {
        const msg: Message = JSON.parse(message.body)
        if (hasSeenMessage(msg.id)) {
          return
        }

        syncConversationPreview(msg)

        if (msg.conversationId === conversationId) {
          addMessage(msg)
          clearUnread(msg.conversationId)
        } else {
          markMessageSeen(msg.id)
          incrementUnread(msg.conversationId)
        }
      }
    )

    return () => {
      messageSub?.unsubscribe()
    }
  }, [
    conversationId,
    connectionState,
    subscribe,
    addMessage,
    clearUnread,
    hasSeenMessage,
    incrementUnread,
    markMessageSeen,
    syncConversationPreview,
  ])

  useEffect(() => {
    if (!conversationId || connectionState !== 'connected') return

    const typingSub = subscribe(
      `/topic/conversation.${conversationId}.typing`,
      (message) => {
        const typing: TypingIndicator = JSON.parse(message.body)
        setTyping(typing.userId, typing.typing)
      }
    )

    return () => {
      typingSub?.unsubscribe()
    }
  }, [conversationId, connectionState, subscribe, setTyping])

  useEffect(() => {
    if (!conversationId || connectionState !== 'connected') return

    const ackSub = subscribe('/user/queue/message-ack', () => {
      void refreshActiveConversation()
    })

    return () => {
      ackSub?.unsubscribe()
    }
  }, [connectionState, conversationId, refreshActiveConversation, subscribe])

  useEffect(() => {
    if (!conversationId || connectionState === 'connected') {
      return
    }

    const interval = window.setInterval(() => {
      void refreshActiveConversation()
    }, 10000)

    return () => {
      window.clearInterval(interval)
    }
  }, [connectionState, conversationId, refreshActiveConversation])

  useEffect(() => {
    if (
      conversationId &&
      previousConnectionStateRef.current !== 'connected' &&
      connectionState === 'connected'
    ) {
      void rehydrateConversationState()
    }

    previousConnectionStateRef.current = connectionState
  }, [connectionState, conversationId, rehydrateConversationState])

  const sendMessage = useCallback(async (content: string, imageUrl?: string) => {
    if (!conversationId) return

    if (connectionState === 'connected') {
      const optimisticMessage: Message = {
        id: optimisticMessageIdRef.current,
        conversationId,
        senderId: currentUser?.id ?? 0,
        senderName: currentUser?.displayName ?? currentUser?.email ?? 'You',
        content,
        imageUrl: imageUrl ?? null,
        status: 'SENT',
        createdAt: new Date().toISOString(),
        isOwnMessage: true,
      }

      optimisticMessageIdRef.current -= 1
      addMessage(optimisticMessage)
      clearUnread(conversationId)
      syncConversationPreview(optimisticMessage)

      publish(
        '/app/chat.sendMessage',
        JSON.stringify({ conversationId, content, imageUrl })
      )
      return
    }

    const message = await chatApi.sendMessage(conversationId, { content, imageUrl })
    addMessage(message)
    clearUnread(conversationId)
    syncConversationPreview(message)

    const [latestConversation, latestConversations] = await Promise.all([
      chatApi.getConversation(conversationId),
      chatApi.getConversations(),
    ])

    setConversations(latestConversations.content)
    upsertConversation(latestConversation)
  }, [
    addMessage,
    clearUnread,
    connectionState,
    conversationId,
    currentUser?.displayName,
    currentUser?.email,
    currentUser?.id,
    publish,
    setConversations,
    syncConversationPreview,
    upsertConversation,
  ])

  const emitTyping = useCallback(() => {
    if (!conversationId) return

    publish(
      '/app/chat.typing',
      JSON.stringify({ conversationId, typing: true })
    )

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current)
    }

    typingTimeoutRef.current = setTimeout(() => {
      publish(
        '/app/chat.typing',
        JSON.stringify({ conversationId, typing: false })
      )
    }, TYPING_DEBOUNCE_MS)
  }, [conversationId, publish])

  return {
    sendMessage,
    emitTyping,
    connectionState
  }
}
