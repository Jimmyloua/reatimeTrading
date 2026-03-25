import { useEffect, useState } from 'react'
import { useWebSocket } from './useWebSocket'
import {
  consumeSellerOnlineToast,
  dropSellerPresenceSubscription,
  ensureSellerPresenceSubscription,
  getSellerPresenceCopy,
  getSellerPresenceSnapshot,
  getSellerPresenceStatusClassName,
  releaseSellerPresence,
  retainSellerPresence,
  seedSellerPresence,
  subscribeToSellerPresence,
  syncSellerPresenceTransport,
  type SellerPresenceStatus,
} from '@/stores/sellerPresenceStore'

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
  const [snapshot, setSnapshot] = useState(() =>
    getSellerPresenceSnapshot(otherUserId, {
      online: initialOnline,
      lastSeenText: initialLastSeen,
    }),
  )

  useEffect(() => {
    const fallback = {
      online: initialOnline,
      lastSeenText: initialLastSeen,
    }
    seedSellerPresence(otherUserId, fallback)
    setSnapshot(getSellerPresenceSnapshot(otherUserId, fallback))
  }, [initialLastSeen, initialOnline, otherUserId])

  useEffect(() => {
    retainSellerPresence(otherUserId, {
      online: initialOnline,
      lastSeenText: initialLastSeen,
    })

    return () => {
      releaseSellerPresence(otherUserId)
    }
  }, [initialLastSeen, initialOnline, otherUserId])

  useEffect(() => {
    return subscribeToSellerPresence(otherUserId, () => {
      setSnapshot(getSellerPresenceSnapshot(otherUserId, {
        online: initialOnline,
        lastSeenText: initialLastSeen,
      }))
    })
  }, [initialLastSeen, initialOnline, otherUserId])

  useEffect(() => {
    syncSellerPresenceTransport(otherUserId, connectionState)

    if (connectionState === 'connected') {
      ensureSellerPresenceSubscription(otherUserId, subscribe, {
        online: initialOnline,
        lastSeenText: initialLastSeen,
      })
    } else {
      dropSellerPresenceSubscription(otherUserId)
    }
  }, [connectionState, initialLastSeen, initialOnline, otherUserId, subscribe])

  return {
    isOnline: snapshot.status === 'online',
    lastSeenText: snapshot.lastSeenText,
    status: snapshot.status as SellerPresenceStatus,
    statusCopy: getSellerPresenceCopy(snapshot),
    statusDotClassName: getSellerPresenceStatusClassName(snapshot.status),
    consumeOnlineToast: () => consumeSellerOnlineToast(otherUserId),
  }
}
