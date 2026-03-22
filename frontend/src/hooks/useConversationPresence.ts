import { useEffect, useState } from 'react'
import { useWebSocket } from './useWebSocket'
import type { PresenceUpdate } from '@/types/chat'

interface UseConversationPresenceOptions {
  otherUserId: number
  initialOnline: boolean
  initialLastSeen: string
}

export function useConversationPresence({
  otherUserId,
  initialOnline,
  initialLastSeen,
}: UseConversationPresenceOptions) {
  const { subscribe, connectionState } = useWebSocket()
  const [isOnline, setIsOnline] = useState(initialOnline)
  const [lastSeenText, setLastSeenText] = useState(initialLastSeen)

  useEffect(() => {
    setIsOnline(initialOnline)
    setLastSeenText(initialLastSeen)
  }, [initialLastSeen, initialOnline, otherUserId])

  useEffect(() => {
    if (connectionState !== 'connected') {
      return
    }

    const subscription = subscribe(`/topic/presence.${otherUserId}`, (message) => {
      const update: PresenceUpdate = JSON.parse(message.body)
      setIsOnline(update.online)
      setLastSeenText(update.lastSeenText)
    })

    return () => {
      subscription?.unsubscribe()
    }
  }, [connectionState, otherUserId, subscribe])

  return {
    isOnline,
    lastSeenText,
  }
}
