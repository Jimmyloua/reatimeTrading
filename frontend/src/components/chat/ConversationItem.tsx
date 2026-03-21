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
      className={`flex items-center gap-3 p-3 cursor-pointer rounded-lg transition-colors ${
        isActive ? 'bg-neutral-100' : 'hover:bg-neutral-50'
      }`}
    >
      <Avatar className="h-10 w-10">
        <AvatarImage src={conversation.otherUserAvatar || undefined} />
        <AvatarFallback>{initials}</AvatarFallback>
      </Avatar>

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <span className="font-medium truncate">
            {conversation.otherUserName}
          </span>
          {conversation.unreadCount > 0 && (
            <Badge variant="destructive" className="text-xs">
              {conversation.unreadCount}
            </Badge>
          )}
        </div>
        <div className="flex items-center justify-between mt-1">
          <span className="text-sm text-neutral-500 truncate">
            Re: {conversation.listingTitle}
          </span>
          <span className="text-xs text-neutral-400">{timeAgo}</span>
        </div>
      </div>
    </div>
  )
}