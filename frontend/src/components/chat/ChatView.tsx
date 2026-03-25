import { useEffect, useRef } from 'react'
import { useChatStore } from '@/stores/chatStore'
import { useChat } from '@/hooks/useChat'
import { useConversationPresence } from '@/hooks/useConversationPresence'
import { chatApi } from '@/api/chatApi'
import { MessageBubble } from './MessageBubble'
import { MessageInput } from './MessageInput'
import { TypingIndicator } from './TypingIndicator'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Skeleton } from '@/components/ui/skeleton'
import { ExternalLink, Radio, WifiOff } from 'lucide-react'
import { buildHeroBackground, HERO_IMAGES } from '@/lib/heroBackgrounds'
import type { Conversation } from '@/types/chat'
import { toast } from 'sonner'

interface ChatViewProps {
  conversation: Conversation
}

export function ChatView({ conversation }: ChatViewProps) {
  const { messages, typingUsers, setMessages, setLoading, isLoading } = useChatStore()
  const { sendMessage, emitTyping, connectionState } = useChat(conversation.id)
  const {
    isOnline: isSellerOnline,
    statusCopy,
    statusDotClassName,
    consumeOnlineToast,
  } = useConversationPresence({
    otherUserId: conversation.otherUserId,
    initialOnline: conversation.otherUserOnline ?? false,
    initialLastSeen: conversation.otherUserLastSeen ?? 'Status updating',
  })
  const scrollRef = useRef<HTMLDivElement>(null)
  const previousPresenceRef = useRef(isSellerOnline)

  // Load messages
  useEffect(() => {
    const loadMessages = async () => {
      setLoading(true)
      try {
        const response = await chatApi.getMessages(conversation.id)
        setMessages(response.content.reverse())
      } catch (error) {
        console.error('Failed to load messages:', error)
      } finally {
        setLoading(false)
      }
    }

    loadMessages()
  }, [conversation.id, setMessages, setLoading])

  // Auto-scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages])

  useEffect(() => {
    if (!previousPresenceRef.current && isSellerOnline && consumeOnlineToast()) {
      toast.success(`${conversation.otherUserName} is online. You can chat now.`)
    }

    previousPresenceRef.current = isSellerOnline
  }, [consumeOnlineToast, conversation.otherUserName, isSellerOnline])

  const handleSend = async (content: string, imageUrl?: string) => {
    try {
      await sendMessage(content, imageUrl)
    } catch (error) {
      console.error('Failed to send message:', error)
    }
    emitTyping()
  }

  const canSend = true
  const statusMessage =
    connectionState === 'connected'
      ? null
      : connectionState === 'reconnecting'
        ? 'Reconnecting chat now. Messages will send again as soon as the connection returns.'
        : connectionState === 'connecting'
          ? 'Connecting chat now...'
          : 'Live chat is offline right now, but messages will still send and the conversation will keep refreshing.'

  return (
    <div
      className="relative flex h-full flex-col overflow-hidden rounded-[1.75rem] border border-white/45 bg-white/70 shadow-[0_28px_90px_rgba(15,23,42,0.12)] backdrop-blur-xl"
      style={buildHeroBackground(HERO_IMAGES.dashboard)}
    >
      <div className="absolute inset-0 bg-[linear-gradient(180deg,rgba(255,255,255,0.2),rgba(248,250,252,0.92)_16%,rgba(248,250,252,0.96)_100%)]" />
      {/* Header */}
      <div className="relative border-b border-slate-200/80 bg-white/70 p-4 backdrop-blur-xl">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">{conversation.otherUserName}</h2>
            <p className="mt-1 text-xs font-medium uppercase tracking-[0.22em] text-slate-400">
              Direct conversation
            </p>
          </div>
          <div className="flex flex-wrap items-center justify-end gap-2">
            <div className="flex items-center gap-2 rounded-full border border-white/60 bg-white/80 px-3 py-1 text-xs font-medium text-slate-600 shadow-sm">
              {connectionState === 'connected' ? (
                <Radio className="h-3.5 w-3.5 text-emerald-500" />
              ) : (
                <WifiOff className="h-3.5 w-3.5 text-amber-500" />
              )}
              {connectionState === 'connected' ? 'Chat live' : connectionState}
            </div>
            <div className="rounded-full border border-white/60 bg-white/80 px-3 py-1 text-xs font-medium text-slate-600 shadow-sm">
              <span className="inline-flex items-center gap-2">
                <span className={`h-2 w-2 rounded-full ${statusDotClassName}`} aria-hidden="true" />
                <span>{statusCopy}</span>
              </span>
            </div>
          </div>
        </div>
        <div className="mt-3">
          <a
            href={`/listings/${conversation.listingId}`}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-sm text-blue-700 hover:underline"
          >
            Re: {conversation.listingTitle}
            <ExternalLink className="h-3 w-3" />
          </a>
        </div>
      </div>

      {/* Messages */}
      <ScrollArea className="relative flex-1 px-4 py-5" ref={scrollRef}>
        {isLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-16 w-2/3 rounded-2xl" />
            ))}
          </div>
        ) : messages.length === 0 ? (
          <div className="flex h-full min-h-[320px] items-center justify-center">
            <div className="max-w-sm rounded-[1.5rem] border border-white/70 bg-white/80 px-6 py-8 text-center shadow-lg backdrop-blur-xl">
              <p className="text-lg font-semibold text-slate-900">Start the conversation</p>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                Ask about condition, pickup, shipping, or price details. Your messages will appear here in real time.
              </p>
            </div>
          </div>
        ) : (
          messages.map((message) => (
            <MessageBubble key={message.id} message={message} />
          ))
        )}
      </ScrollArea>

      {/* Typing indicator */}
      <div className="relative">
        <TypingIndicator
          typingUsers={typingUsers}
          users={new Map([[conversation.otherUserId, { name: conversation.otherUserName }]])}
        />
      </div>

      {/* Input */}
      <MessageInput
        onSend={handleSend}
        onTyping={emitTyping}
        canSend={canSend}
        statusMessage={statusMessage}
      />
    </div>
  )
}
