import { useState } from 'react'
import { useChatStore } from '@/stores/chatStore'
import { ConversationList } from '@/components/chat/ConversationList'
import { ChatView } from '@/components/chat/ChatView'

export default function MessagesPage() {
  const [activeConversationId, setActiveConversationId] = useState<number | null>(null)
  const { conversations } = useChatStore()

  const activeConversation = conversations.find(c => c.id === activeConversationId) || null

  return (
    <div className="container mx-auto h-[calc(100vh-4rem)] flex gap-4 p-4">
      {/* Conversation List - Left Sidebar */}
      <div className="w-[280px] border rounded-lg overflow-hidden flex-shrink-0">
        <div className="border-b p-4">
          <h1 className="text-lg font-semibold">Messages</h1>
        </div>
        <div className="h-[calc(100%-60px)] overflow-y-auto">
          <ConversationList
            onSelectConversation={setActiveConversationId}
            activeConversationId={activeConversationId}
          />
        </div>
      </div>

      {/* Chat View - Right Side */}
      <div className="flex-1 border rounded-lg overflow-hidden">
        {activeConversation ? (
          <ChatView conversation={activeConversation} />
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