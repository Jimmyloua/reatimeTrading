import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import type { Conversation } from '@/types/chat'

interface ConversationItemProps {
  conversation: Conversation
  isActive: boolean
  onClick: () => void
}

export function ConversationItem({ conversation, isActive, onClick }: ConversationItemProps) {
  const initials = conversation.otherUserName
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)

  const timeAgo = conversation.lastMessageAt
    ? new Date(conversation.lastMessageAt).toLocaleDateString()
    : ''

  return (
    <div
      onClick={onClick}
      className={`flex items-center gap-3 rounded-[1.25rem] border px-3 py-3 cursor-pointer transition-all ${
        isActive
          ? 'border-slate-900/10 bg-white shadow-md shadow-slate-900/8'
          : 'border-transparent bg-white/55 hover:border-white/60 hover:bg-white/75'
      }`}
    >
      <Avatar className="h-10 w-10">
        <AvatarImage src={conversation.otherUserAvatar || undefined} />
        <AvatarFallback>{initials}</AvatarFallback>
      </Avatar>

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <span className="font-medium truncate text-slate-900">
            {conversation.otherUserName}
          </span>
          {conversation.unreadCount > 0 && (
            <Badge variant="destructive" className="text-xs">
              {conversation.unreadCount}
            </Badge>
          )}
        </div>
        <div className="flex items-center justify-between mt-1">
          <span className="text-sm text-slate-500 truncate">
            Re: {conversation.listingTitle}
          </span>
          <span className="text-xs text-slate-400">{timeAgo}</span>
        </div>
        {conversation.lastMessage ? (
          <p className="mt-2 truncate text-xs text-slate-500">{conversation.lastMessage}</p>
        ) : null}
      </div>
    </div>
  )
}
