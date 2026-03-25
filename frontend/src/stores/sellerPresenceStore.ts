import type { IMessage, StompSubscription } from '@stomp/stompjs'
import type { ConnectionState } from '@/hooks/useWebSocket'
import type { PresenceUpdate } from '@/types/chat'

export type SellerPresenceStatus = 'online' | 'offline' | 'stale'

export interface SellerPresenceSnapshot {
  status: SellerPresenceStatus
  lastSeenText: string
  updatedAt: number
}

interface SellerPresenceSeed {
  online: boolean
  lastSeenText: string
  updatedAt?: string | number
}

interface SellerPresenceEntry {
  snapshot: SellerPresenceSnapshot
  listeners: Set<() => void>
  subscribers: number
  socketSubscription: StompSubscription | null
  staleTimeout: ReturnType<typeof setTimeout> | null
}

type PresenceSubscriber = (destination: string, callback: (message: IMessage) => void) => StompSubscription | null

const STALE_WINDOW_MS = 30000
const STATUS_UPDATING_COPY = 'Status updating'
const sellerPresenceEntries = new Map<number, SellerPresenceEntry>()
const sellerToastGuards = new Set<number>()

function createSnapshot(seed: SellerPresenceSeed): SellerPresenceSnapshot {
  return {
    status: seed.online ? 'online' : 'offline',
    lastSeenText: seed.lastSeenText,
    updatedAt: toTimestamp(seed.updatedAt),
  }
}

function createEntry(seed: SellerPresenceSeed): SellerPresenceEntry {
  return {
    snapshot: createSnapshot(seed),
    listeners: new Set(),
    subscribers: 0,
    socketSubscription: null,
    staleTimeout: null,
  }
}

function getOrCreateEntry(otherUserId: number, seed: SellerPresenceSeed): SellerPresenceEntry {
  const existing = sellerPresenceEntries.get(otherUserId)
  if (existing) {
    return existing
  }

  const entry = createEntry(seed)
  sellerPresenceEntries.set(otherUserId, entry)
  return entry
}

function toTimestamp(updatedAt?: string | number) {
  if (typeof updatedAt === 'number' && Number.isFinite(updatedAt)) {
    return updatedAt
  }

  if (typeof updatedAt === 'string') {
    const parsed = Date.parse(updatedAt)
    if (!Number.isNaN(parsed)) {
      return parsed
    }
  }

  return Date.now()
}

function notify(otherUserId: number) {
  const entry = sellerPresenceEntries.get(otherUserId)
  entry?.listeners.forEach((listener) => listener())
}

function clearStaleTimeout(entry: SellerPresenceEntry) {
  if (entry.staleTimeout) {
    clearTimeout(entry.staleTimeout)
    entry.staleTimeout = null
  }
}

function setSnapshot(otherUserId: number, nextSnapshot: SellerPresenceSnapshot) {
  const entry = sellerPresenceEntries.get(otherUserId)
  if (!entry) {
    return
  }

  if (
    entry.snapshot.status === nextSnapshot.status &&
    entry.snapshot.lastSeenText === nextSnapshot.lastSeenText &&
    entry.snapshot.updatedAt === nextSnapshot.updatedAt
  ) {
    return
  }

  entry.snapshot = nextSnapshot
  notify(otherUserId)
}

function applyPresenceUpdate(otherUserId: number, update: PresenceUpdate | SellerPresenceSeed) {
  const entry = getOrCreateEntry(otherUserId, update)
  clearStaleTimeout(entry)

  setSnapshot(otherUserId, {
    status: update.online ? 'online' : 'offline',
    lastSeenText: update.lastSeenText,
    updatedAt: toTimestamp(update.updatedAt),
  })
}

export function seedSellerPresence(otherUserId: number, seed: SellerPresenceSeed) {
  const entry = getOrCreateEntry(otherUserId, seed)
  const nextUpdatedAt = toTimestamp(seed.updatedAt)

  if (nextUpdatedAt < entry.snapshot.updatedAt) {
    return
  }

  clearStaleTimeout(entry)
  setSnapshot(otherUserId, {
    status: seed.online ? 'online' : 'offline',
    lastSeenText: seed.lastSeenText,
    updatedAt: nextUpdatedAt,
  })
}

export function retainSellerPresence(otherUserId: number, seed: SellerPresenceSeed) {
  const entry = getOrCreateEntry(otherUserId, seed)
  entry.subscribers += 1
}

export function releaseSellerPresence(otherUserId: number) {
  const entry = sellerPresenceEntries.get(otherUserId)
  if (!entry) {
    return
  }

  entry.subscribers = Math.max(0, entry.subscribers - 1)
  if (entry.subscribers === 0 && entry.socketSubscription) {
    entry.socketSubscription.unsubscribe()
    entry.socketSubscription = null
  }
  if (entry.subscribers === 0) {
    clearStaleTimeout(entry)
  }
}

export function subscribeToSellerPresence(otherUserId: number, listener: () => void) {
  const entry = getOrCreateEntry(otherUserId, {
    online: false,
    lastSeenText: STATUS_UPDATING_COPY,
  })

  entry.listeners.add(listener)

  return () => {
    entry.listeners.delete(listener)
  }
}

export function getSellerPresenceSnapshot(
  otherUserId: number,
  fallback: SellerPresenceSeed,
): SellerPresenceSnapshot {
  return getOrCreateEntry(otherUserId, fallback).snapshot
}

export function ensureSellerPresenceSubscription(
  otherUserId: number,
  subscribe: PresenceSubscriber,
  fallback: SellerPresenceSeed,
) {
  const entry = getOrCreateEntry(otherUserId, fallback)

  if (entry.socketSubscription || entry.subscribers === 0) {
    return
  }

  entry.socketSubscription = subscribe(`/topic/presence.${otherUserId}`, (message) => {
    const update: PresenceUpdate = JSON.parse(message.body)
    applyPresenceUpdate(otherUserId, update)
  })
}

export function dropSellerPresenceSubscription(otherUserId: number) {
  const entry = sellerPresenceEntries.get(otherUserId)
  if (!entry?.socketSubscription) {
    return
  }

  entry.socketSubscription.unsubscribe()
  entry.socketSubscription = null
}

export function syncSellerPresenceTransport(otherUserId: number, connectionState: ConnectionState) {
  const entry = sellerPresenceEntries.get(otherUserId)
  if (!entry) {
    return
  }

  if (connectionState === 'connected') {
    clearStaleTimeout(entry)
    return
  }

  if (entry.snapshot.status === 'stale' || entry.staleTimeout) {
    return
  }

  const elapsed = Date.now() - entry.snapshot.updatedAt
  const remaining = Math.max(STALE_WINDOW_MS - elapsed, 0)

  entry.staleTimeout = setTimeout(() => {
    entry.staleTimeout = null
    setSnapshot(otherUserId, {
      status: 'stale',
      lastSeenText: STATUS_UPDATING_COPY,
      updatedAt: entry.snapshot.updatedAt,
    })
  }, remaining)
}

export function consumeSellerOnlineToast(otherUserId: number) {
  if (sellerToastGuards.has(otherUserId)) {
    return false
  }

  sellerToastGuards.add(otherUserId)
  return true
}

export function getSellerPresenceCopy(snapshot: SellerPresenceSnapshot) {
  if (snapshot.status === 'online') {
    return 'Seller online'
  }

  return snapshot.lastSeenText
}

export function getSellerPresenceStatusClassName(status: SellerPresenceStatus) {
  if (status === 'online') {
    return 'bg-emerald-500'
  }

  if (status === 'stale') {
    return 'bg-amber-500'
  }

  return 'bg-slate-400'
}

export function resetSellerPresenceStore() {
  sellerPresenceEntries.forEach((entry) => {
    clearStaleTimeout(entry)
    entry.socketSubscription?.unsubscribe()
  })
  sellerPresenceEntries.clear()
  sellerToastGuards.clear()
}
