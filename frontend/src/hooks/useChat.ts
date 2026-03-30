import { useCallback, useEffect, useRef } from 'react'
import { chatApi } from '@/api/chatApi'
import { useChatStore } from '@/stores/chatStore'
import { useAuthStore } from '@/stores/authStore'
import { useWebSocket } from './useWebSocket'
import type { Message, MessageAck, TypingIndicator } from '@/types/chat'

const TYPING_DEBOUNCE_MS = 3000

export function useChat(conversationId: number | null) {
  const { subscribe, publish, connectionState } = useWebSocket()
  const currentUser = useAuthStore((state) => state.user)
  const {
    addMessage,
    appendMessages,
    clearUnread,
    getLatestPersistedMessageId,
    hasSeenMessage,
    incrementUnread,
    markMessageSeen,
    reconcileMessageAck,
    setMessages,
    setTyping,
    syncConversationPreview,
  } = useChatStore()
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const previousConnectionStateRef = useRef(connectionState)
  const optimisticMessageIdRef = useRef(-1)
  const clientMessageSequenceRef = useRef(0)

  const refreshActiveConversation = useCallback(async () => {
    if (!conversationId) {
      return
    }

    const latestMessages = await chatApi.getMessages(conversationId)
    setMessages(latestMessages.content.reverse())
    clearUnread(conversationId)
  }, [clearUnread, conversationId, setMessages])

  const catchUpConversation = useCallback(async () => {
    if (!conversationId) {
      return
    }

    const afterMessageId = getLatestPersistedMessageId(conversationId) ?? undefined
    const latestMessages = await chatApi.getMessages(conversationId, 0, 50, afterMessageId)

    if (afterMessageId == null) {
      setMessages(latestMessages.content.reverse())
    } else {
      appendMessages(latestMessages.content)
      latestMessages.content.forEach((message) => syncConversationPreview(message))
    }

    clearUnread(conversationId)
  }, [appendMessages, clearUnread, conversationId, getLatestPersistedMessageId, setMessages, syncConversationPreview])

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

    const ackSub = subscribe('/user/queue/message-ack', (message) => {
      const ack: MessageAck = JSON.parse(message.body)
      const reconciledMessage = reconcileMessageAck(ack)
      if (!reconciledMessage) {
        return
      }

      clearUnread(ack.conversationId)
      syncConversationPreview(reconciledMessage)
    })

    return () => {
      ackSub?.unsubscribe()
    }
  }, [clearUnread, connectionState, conversationId, reconcileMessageAck, subscribe, syncConversationPreview])

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
      void catchUpConversation()
    }

    previousConnectionStateRef.current = connectionState
  }, [catchUpConversation, connectionState, conversationId])

  const createClientMessageId = useCallback(() => {
    clientMessageSequenceRef.current += 1
    return `client-${Date.now()}-${clientMessageSequenceRef.current}`
  }, [])

  const sendMessage = useCallback(async (content: string, imageUrl?: string) => {
    if (!conversationId) return

    const clientMessageId = createClientMessageId()
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
      clientMessageId,
    }

    optimisticMessageIdRef.current -= 1
    addMessage(optimisticMessage)
    clearUnread(conversationId)
    syncConversationPreview(optimisticMessage)

    if (connectionState === 'connected') {
      publish(
        '/app/chat.sendMessage',
        JSON.stringify({ conversationId, content, imageUrl, clientMessageId })
      )
      return
    }

    const ack = await chatApi.sendMessage(conversationId, { content, imageUrl, clientMessageId })
    const reconciledMessage = reconcileMessageAck(ack)
    if (reconciledMessage) {
      clearUnread(conversationId)
      syncConversationPreview(reconciledMessage)
    }
  }, [
    addMessage,
    createClientMessageId,
    clearUnread,
    connectionState,
    conversationId,
    currentUser?.displayName,
    currentUser?.email,
    currentUser?.id,
    publish,
    reconcileMessageAck,
    syncConversationPreview,
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
