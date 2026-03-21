interface TypingIndicatorProps {
  typingUsers: Map<number, boolean>
  users: Map<number, { name: string }>
}

export function TypingIndicator({ typingUsers, users }: TypingIndicatorProps) {
  const typingUserIds = Array.from(typingUsers.entries())
    .filter(([_, isTyping]) => isTyping)
    .map(([userId]) => userId)

  if (typingUserIds.length === 0) return null

  const names = typingUserIds
    .map(id => users.get(id)?.name || 'Someone')
    .join(', ')

  return (
    <div className="px-4 py-2 text-sm text-neutral-500 italic">
      {names} {typingUserIds.length === 1 ? 'is' : 'are'} typing...
    </div>
  )
}