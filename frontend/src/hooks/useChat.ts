import { useCallback, useEffect, useRef } from 'react'
import { useChatStore } from '@/stores/chatStore'
import { useWebSocket } from './useWebSocket'
import type { Message, TypingIndicator } from '@/types/chat'

const TYPING_DEBOUNCE_MS = 3000

export function useChat(conversationId: number | null) {
  const { subscribe, publish, connectionState } = useWebSocket()
  const { addMessage, clearUnread, incrementUnread, setTyping, syncConversationPreview } = useChatStore()
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Subscribe to message updates for both active and inactive conversations.
  useEffect(() => {
    if (connectionState !== 'connected') return

    const messageSub = subscribe(
      `/user/queue/messages`,
      (message) => {
        const msg: Message = JSON.parse(message.body)
        syncConversationPreview(msg)

        if (msg.conversationId === conversationId) {
          addMessage(msg)
          clearUnread(msg.conversationId)
        } else {
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
    incrementUnread,
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

  const sendMessage = useCallback((content: string, imageUrl?: string) => {
    if (!conversationId) return

    publish(
      '/app/chat.sendMessage',
      JSON.stringify({ conversationId, content, imageUrl })
    )
  }, [conversationId, publish])

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
