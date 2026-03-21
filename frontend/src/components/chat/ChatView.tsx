import { useEffect, useRef } from 'react'
import { useChatStore } from '@/stores/chatStore'
import { useChat } from '@/hooks/useChat'
import { chatApi } from '@/api/chatApi'
import { MessageBubble } from './MessageBubble'
import { MessageInput } from './MessageInput'
import { TypingIndicator } from './TypingIndicator'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Skeleton } from '@/components/ui/skeleton'
import { ExternalLink } from 'lucide-react'
import type { Conversation } from '@/types/chat'

interface ChatViewProps {
  conversation: Conversation
}

export function ChatView({ conversation }: ChatViewProps) {
  const { messages, typingUsers, setMessages, setLoading, isLoading } = useChatStore()
  const { sendMessage, emitTyping, connectionState } = useChat(conversation.id)
  const scrollRef = useRef<HTMLDivElement>(null)

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

  const handleSend = (content: string, imageUrl?: string) => {
    sendMessage(content, imageUrl)
    emitTyping()
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b p-4 flex items-center justify-between">
        <div>
          <h2 className="font-semibold">{conversation.otherUserName}</h2>
          <a
            href={`/listings/${conversation.listingId}`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm text-blue-600 hover:underline flex items-center gap-1"
          >
            Re: {conversation.listingTitle}
            <ExternalLink className="h-3 w-3" />
          </a>
        </div>
        {connectionState === 'reconnecting' && (
          <span className="text-sm text-yellow-600">Reconnecting...</span>
        )}
      </div>

      {/* Messages */}
      <ScrollArea className="flex-1 p-4" ref={scrollRef}>
        {isLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-16 w-2/3 rounded-2xl" />
            ))}
          </div>
        ) : (
          messages.map((message) => (
            <MessageBubble key={message.id} message={message} />
          ))
        )}
      </ScrollArea>

      {/* Typing indicator */}
      <TypingIndicator
        typingUsers={typingUsers}
        users={new Map([[conversation.otherUserId, { name: conversation.otherUserName }]])}
      />

      {/* Input */}
      <MessageInput
        onSend={handleSend}
        onTyping={emitTyping}
        disabled={connectionState !== 'connected'}
      />
    </div>
  )
}