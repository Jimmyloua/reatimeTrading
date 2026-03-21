import { useCallback, useEffect, useRef, useState } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/stores/authStore'

export type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'reconnecting'

export function useWebSocket() {
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected')
  const clientRef = useRef<Client | null>(null)
  const reconnectAttempts = useRef(0)

  const MAX_RECONNECT_DELAY = 30000
  const BASE_DELAY = 1000

  const connect = useCallback(() => {
    const token = useAuthStore.getState().accessToken
    if (!token) {
      console.warn('No auth token, skipping WebSocket connection')
      return
    }

    setConnectionState('connecting')

    const client = new Client({
      webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        setConnectionState('connected')
        reconnectAttempts.current = 0
        console.log('WebSocket connected')
      },
      onDisconnect: () => {
        setConnectionState('disconnected')
        console.log('WebSocket disconnected')
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      },
      onWebSocketClose: () => {
        if (reconnectAttempts.current < 5) {
          setConnectionState('reconnecting')
          const delay = Math.min(
            BASE_DELAY * Math.pow(2, reconnectAttempts.current),
            MAX_RECONNECT_DELAY
          )
          reconnectAttempts.current++
          setTimeout(connect, delay)
        } else {
          setConnectionState('disconnected')
        }
      },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    client.activate()
    clientRef.current = client
  }, [])

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate()
      clientRef.current = null
    }
    setConnectionState('disconnected')
  }, [])

  const subscribe = useCallback((destination: string, callback: (message: IMessage) => void) => {
    if (clientRef.current?.connected) {
      return clientRef.current.subscribe(destination, callback)
    }
    return null
  }, [])

  const publish = useCallback((destination: string, body: string) => {
    if (clientRef.current?.connected) {
      clientRef.current.publish({ destination, body })
    }
  }, [])

  useEffect(() => {
    const token = useAuthStore.getState().accessToken
    if (token && !clientRef.current?.connected) {
      connect()
    }

    return () => {
      disconnect()
    }
  }, [connect, disconnect])

  return {
    client: clientRef.current,
    connectionState,
    connect,
    disconnect,
    subscribe,
    publish
  }
}