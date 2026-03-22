import { useCallback, useEffect, useState } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/stores/authStore'

export type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'reconnecting'

type ConnectionListener = (state: ConnectionState) => void

const listeners = new Set<ConnectionListener>()
const MAX_RECONNECT_DELAY = 30000
const BASE_DELAY = 1000
const HEARTBEAT_INTERVAL_MS = 30000

let client: Client | null = null
let currentToken: string | null = null
let connectionState: ConnectionState = 'disconnected'
let reconnectAttempts = 0
let reconnectTimeout: ReturnType<typeof setTimeout> | null = null
let heartbeatInterval: ReturnType<typeof setInterval> | null = null
let intentionalDisconnect = false

function getWebSocketBaseUrl() {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL
  }

  return window.location.origin
}

function notifyConnectionListeners() {
  listeners.forEach((listener) => listener(connectionState))
}

function setSharedConnectionState(nextState: ConnectionState) {
  if (connectionState === nextState) {
    return
  }

  connectionState = nextState
  notifyConnectionListeners()
}

function clearReconnectTimeout() {
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout)
    reconnectTimeout = null
  }
}

function stopHeartbeat() {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval)
    heartbeatInterval = null
  }
}

function startHeartbeat() {
  stopHeartbeat()

  heartbeatInterval = setInterval(() => {
    if (client?.connected) {
      client.publish({ destination: '/app/chat.heartbeat', body: '{}' })
    }
  }, HEARTBEAT_INTERVAL_MS)
}

function scheduleReconnect() {
  if (!currentToken || reconnectAttempts >= 5 || reconnectTimeout) {
    if (reconnectAttempts >= 5) {
      setSharedConnectionState('disconnected')
    }
    return
  }

  setSharedConnectionState('reconnecting')
  const delay = Math.min(BASE_DELAY * Math.pow(2, reconnectAttempts), MAX_RECONNECT_DELAY)
  reconnectAttempts += 1

  reconnectTimeout = setTimeout(() => {
    reconnectTimeout = null
    if (currentToken) {
      ensureConnected(currentToken)
    }
  }, delay)
}

function createClient(accessToken: string) {
  const socketClient = new Client({
    webSocketFactory: () => new SockJS(`${getWebSocketBaseUrl()}/ws`),
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`,
    },
    onConnect: () => {
      if (client !== socketClient) {
        return
      }
      reconnectAttempts = 0
      setSharedConnectionState('connected')
      startHeartbeat()
    },
    onDisconnect: () => {
      if (client !== socketClient) {
        return
      }
      stopHeartbeat()
      if (intentionalDisconnect || !currentToken) {
        setSharedConnectionState('disconnected')
      }
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame)
    },
    onWebSocketClose: () => {
      if (client !== socketClient) {
        return
      }
      stopHeartbeat()
      client = null
      if (intentionalDisconnect || !currentToken) {
        setSharedConnectionState('disconnected')
        return
      }

      scheduleReconnect()
    },
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })

  return socketClient
}

function ensureConnected(accessToken: string) {
  if (!accessToken) {
    disconnectShared()
    return
  }

  if (currentToken !== accessToken && client) {
    disconnectShared(false)
  }

  currentToken = accessToken
  intentionalDisconnect = false

  if (client && (client.active || client.connected)) {
    return
  }

  clearReconnectTimeout()
  setSharedConnectionState(reconnectAttempts > 0 ? 'reconnecting' : 'connecting')
  client = createClient(accessToken)
  client.activate()
}

function disconnectShared(clearToken = true) {
  intentionalDisconnect = true
  clearReconnectTimeout()
  stopHeartbeat()

  if (clearToken) {
    currentToken = null
    reconnectAttempts = 0
  }

  if (client) {
    const activeClient = client
    client = null
    void activeClient.deactivate()
  }

  setSharedConnectionState('disconnected')
}

function addConnectionListener(listener: ConnectionListener) {
  listeners.add(listener)
  listener(connectionState)

  return () => {
    listeners.delete(listener)
  }
}

export function useWebSocket() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const [localConnectionState, setLocalConnectionState] = useState<ConnectionState>(connectionState)

  useEffect(() => addConnectionListener(setLocalConnectionState), [])

  useEffect(() => {
    if (accessToken) {
      ensureConnected(accessToken)
      return
    }

    disconnectShared()
  }, [accessToken])

  const subscribe = useCallback((destination: string, callback: (message: IMessage) => void) => {
    if (client?.connected) {
      return client.subscribe(destination, callback)
    }

    return null
  }, [])

  const publish = useCallback((destination: string, body: string) => {
    if (client?.connected) {
      client.publish({ destination, body })
    }
  }, [])

  const connect = useCallback(() => {
    if (accessToken) {
      ensureConnected(accessToken)
    }
  }, [accessToken])

  const disconnect = useCallback(() => {
    disconnectShared()
  }, [])

  return {
    client,
    connectionState: localConnectionState,
    connect,
    disconnect,
    subscribe,
    publish,
  }
}
