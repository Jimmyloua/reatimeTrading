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
          'max-w-[85%] md:max-w-[70%] overflow-hidden rounded-[1.5rem] px-4 py-3 shadow-sm backdrop-blur-sm',
          isOwn
            ? 'bg-slate-900 text-white rounded-br-md shadow-slate-900/15'
            : 'border border-white/80 bg-white/85 text-slate-800 rounded-bl-md'
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
          <p className="text-sm leading-6 whitespace-pre-wrap break-words [overflow-wrap:anywhere]">{message.content}</p>
        )}
        <div className={cn('text-xs mt-2', isOwn ? 'text-slate-300' : 'text-slate-500')}>
          {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </div>
      </div>
    </div>
  )
}
