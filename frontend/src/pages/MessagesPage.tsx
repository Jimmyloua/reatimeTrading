import { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { chatApi } from '@/api/chatApi'
import { useChatStore } from '@/stores/chatStore'
import { ConversationList } from '@/components/chat/ConversationList'
import { ChatView } from '@/components/chat/ChatView'

export default function MessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const {
    conversations,
    activeConversation,
    clearUnread,
    setActiveConversation,
    upsertConversation,
  } = useChatStore()

  const conversationParam = searchParams.get('conversation')
  const activeConversationId =
    conversationParam && /^\d+$/.test(conversationParam)
      ? Number.parseInt(conversationParam, 10)
      : null

  const matchedConversation = conversations.find((conversation) => conversation.id === activeConversationId) || null
  const selectedConversation =
    activeConversation?.id === activeConversationId ? activeConversation : matchedConversation

  useEffect(() => {
    if (activeConversationId === null) {
      setActiveConversation(null)
      return
    }

    if (matchedConversation) {
      if (matchedConversation.unreadCount > 0) {
        clearUnread(activeConversationId)
      }
      setActiveConversation({
        ...matchedConversation,
        unreadCount: 0,
      })
      return
    }

    let cancelled = false

    const bootstrapConversation = async () => {
      try {
        const conversation = await chatApi.getConversation(activeConversationId)
        if (cancelled) {
          return
        }

        upsertConversation(conversation)
        setActiveConversation({
          ...conversation,
          unreadCount: 0,
        })
      } catch (error) {
        console.error('Failed to load conversation:', error)
      }
    }

    void bootstrapConversation()

    return () => {
      cancelled = true
    }
  }, [activeConversationId, clearUnread, matchedConversation, setActiveConversation, upsertConversation])

  const handleSelectConversation = (conversationId: number) => {
    setSearchParams({ conversation: String(conversationId) })
    clearUnread(conversationId)
  }

  return (
    <div className="container mx-auto h-[calc(100vh-4rem)] flex gap-4 p-4">
      {/* Conversation List - Left Sidebar */}
      <div className="w-[280px] border rounded-lg overflow-hidden flex-shrink-0">
        <div className="border-b p-4">
          <h1 className="text-lg font-semibold">Messages</h1>
        </div>
        <div className="h-[calc(100%-60px)] overflow-y-auto">
          <ConversationList
            onSelectConversation={handleSelectConversation}
            activeConversationId={activeConversationId}
          />
        </div>
      </div>

      {/* Chat View - Right Side */}
      <div className="flex-1 border rounded-lg overflow-hidden">
        {selectedConversation ? (
          <ChatView conversation={selectedConversation} />
        ) : (
          <div className="h-full flex items-center justify-center text-neutral-500">
            <div className="text-center">
              <p className="text-lg">Select a conversation</p>
              <p className="text-sm mt-1">Choose a conversation from the list to start chatting</p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
