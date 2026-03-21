import { useEffect } from 'react'
import { useChatStore } from '@/stores/chatStore'
import { chatApi } from '@/api/chatApi'
import { ConversationItem } from './ConversationItem'
import { Skeleton } from '@/components/ui/skeleton'

interface ConversationListProps {
  onSelectConversation: (conversationId: number) => void
  activeConversationId: number | null
}

export function ConversationList({ onSelectConversation, activeConversationId }: ConversationListProps) {
  const { conversations, isLoading, setConversations, setLoading, setError } = useChatStore()

  useEffect(() => {
    const fetchConversations = async () => {
      setLoading(true)
      try {
        const response = await chatApi.getConversations()
        setConversations(response.content)
      } catch (error) {
        setError('Failed to load conversations')
      } finally {
        setLoading(false)
      }
    }

    fetchConversations()
  }, [setConversations, setLoading, setError])

  if (isLoading) {
    return (
      <div className="p-4 space-y-3">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-full" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-3 w-32" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (conversations.length === 0) {
    return (
      <div className="p-4 text-center">
        <p className="text-neutral-500">No conversations</p>
        <p className="text-sm text-neutral-400 mt-1">
          When you contact a seller, conversations will appear here.
        </p>
      </div>
    )
  }

  return (
    <div className="divide-y">
      {conversations.map((conversation) => (
        <ConversationItem
          key={conversation.id}
          conversation={conversation}
          isActive={conversation.id === activeConversationId}
          onClick={() => onSelectConversation(conversation.id)}
        />
      ))}
    </div>
  )
}