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
    <div className="border-t border-slate-200/60 bg-white/70 px-4 py-2 text-sm italic text-slate-500 backdrop-blur-sm">
      {names} {typingUserIds.length === 1 ? 'is' : 'are'} typing...
    </div>
  )
}
