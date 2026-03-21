import type { Message } from '@/types/chat'
import { cn } from '@/lib/utils'

interface MessageBubbleProps {
  message: Message
}

export function MessageBubble({ message }: MessageBubbleProps) {
  const isOwn = message.isOwnMessage

  return (
    <div className={cn('flex mb-3', isOwn ? 'justify-end' : 'justify-start')}>
      <div
        className={cn(
          'max-w-[70%] rounded-2xl px-4 py-2',
          isOwn
            ? 'bg-neutral-800 text-white rounded-br-md'
            : 'bg-neutral-100 rounded-bl-md'
        )}
      >
        {message.imageUrl && (
          <img
            src={message.imageUrl}
            alt="Shared image"
            className="max-w-full rounded-lg mb-2"
          />
        )}
        {message.content && (
          <p className="text-sm whitespace-pre-wrap">{message.content}</p>
        )}
        <div className={cn('text-xs mt-1', isOwn ? 'text-neutral-400' : 'text-neutral-500')}>
          {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </div>
      </div>
    </div>
  )
}