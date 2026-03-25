import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { useConversationPresence } from '@/hooks/useConversationPresence'

const websocketMock = vi.hoisted(() => {
  let connectionState: 'connecting' | 'connected' | 'disconnected' | 'reconnecting' = 'connected'
  const subscribers = new Map<string, Array<(message: { body: string }) => void>>()

  return {
    subscribe: vi.fn((destination: string, callback: (message: { body: string }) => void) => {
      const existing = subscribers.get(destination) ?? []
      existing.push(callback)
      subscribers.set(destination, existing)

      return {
        unsubscribe: () => {
          subscribers.set(
            destination,
            (subscribers.get(destination) ?? []).filter((current) => current !== callback),
          )
        },
      }
    }),
    emit(destination: string, payload: unknown) {
      for (const callback of subscribers.get(destination) ?? []) {
        callback({ body: JSON.stringify(payload) })
      }
    },
    getConnectionState() {
      return connectionState
    },
    setConnectionState(nextState: 'connecting' | 'connected' | 'disconnected' | 'reconnecting') {
      connectionState = nextState
    },
    reset() {
      connectionState = 'connected'
      subscribers.clear()
      this.subscribe.mockClear()
    },
  }
})

vi.mock('@/hooks/useWebSocket', () => ({
  useWebSocket: () => ({
    subscribe: websocketMock.subscribe,
    connectionState: websocketMock.getConnectionState(),
  }),
}))

function PresenceLabel({
  label,
  otherUserId,
  initialOnline = false,
  initialLastSeen = 'Last seen 5m ago',
}: {
  label: string
  otherUserId: number
  initialOnline?: boolean
  initialLastSeen?: string
}) {
  const { isOnline, lastSeenText } = useConversationPresence({
    otherUserId,
    initialOnline,
    initialLastSeen,
  })

  return <div>{label}: {isOnline ? 'Seller online' : lastSeenText}</div>
}

describe('shared seller presence contract', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    websocketMock.reset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  test('one shared subscription should drive every matching seller row and the active header in the same cycle', () => {
    render(
      <>
        <PresenceLabel label="row-a" otherUserId={42} />
        <PresenceLabel label="row-b" otherUserId={42} />
        <PresenceLabel label="header" otherUserId={42} />
      </>,
    )

    act(() => {
      websocketMock.emit('/topic/presence.42', {
        userId: 42,
        online: true,
        lastSeenText: 'Last seen just now',
      })
    })

    expect(screen.getByText('row-a: Seller online')).toBeInTheDocument()
    expect(screen.getByText('row-b: Seller online')).toBeInTheDocument()
    expect(screen.getByText('header: Seller online')).toBeInTheDocument()
    expect(websocketMock.subscribe).toHaveBeenCalledTimes(1)
  })

  test('presence should degrade to Status updating after the disconnect stale window expires', () => {
    const view = render(
      <PresenceLabel label="header" otherUserId={42} initialOnline />,
    )

    expect(screen.getByText('header: Seller online')).toBeInTheDocument()

    websocketMock.setConnectionState('disconnected')

    act(() => {
      vi.advanceTimersByTime(30_001)
    })

    view.rerender(<PresenceLabel label="header" otherUserId={42} initialOnline />)

    expect(screen.getByText('header: Status updating')).toBeInTheDocument()
  })
})
