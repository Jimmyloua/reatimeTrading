import { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { chatApi } from '@/api/chatApi'
import { useChatStore } from '@/stores/chatStore'
import { ConversationList } from '@/components/chat/ConversationList'
import { ChatView } from '@/components/chat/ChatView'
import { buildHeroBackground, HERO_IMAGES } from '@/lib/heroBackgrounds'

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
    <div
      className="container mx-auto min-h-[calc(100vh-4rem)] px-4 py-6"
      style={buildHeroBackground(HERO_IMAGES.dashboard)}
    >
      <div className="mb-6 rounded-[2rem] border border-white/30 bg-slate-950/55 px-6 py-7 text-white shadow-[0_28px_90px_rgba(15,23,42,0.28)] backdrop-blur-sm">
        <p className="text-sm font-medium uppercase tracking-[0.28em] text-cyan-200/80">Messages</p>
        <h1 className="mt-3 text-3xl font-semibold md:text-4xl">Messages</h1>
        <p className="mt-2 text-lg font-medium text-slate-100">Talk to multiple sellers in one place</p>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-200 md:text-base">
          Jump between product threads, follow replies in real time, and keep every negotiation attached to its listing.
        </p>
      </div>

      <div className="flex h-[calc(100vh-14rem)] gap-4">
      {/* Conversation List - Left Sidebar */}
      <div className="w-[320px] overflow-hidden rounded-[1.75rem] border border-white/40 bg-white/70 shadow-[0_28px_90px_rgba(15,23,42,0.12)] backdrop-blur-xl flex-shrink-0">
        <div className="border-b border-slate-200/80 bg-white/75 p-4 backdrop-blur-sm">
          <h2 className="text-lg font-semibold text-slate-900">Active chats</h2>
          <p className="mt-1 text-sm text-slate-500">Choose a seller conversation to continue.</p>
        </div>
        <div className="h-[calc(100%-60px)] overflow-y-auto">
          <ConversationList
            onSelectConversation={handleSelectConversation}
            activeConversationId={activeConversationId}
          />
        </div>
      </div>

      {/* Chat View - Right Side */}
      <div className="flex-1 overflow-hidden rounded-[1.75rem]">
        {selectedConversation ? (
          <ChatView conversation={selectedConversation} />
        ) : (
          <div className="flex h-full items-center justify-center rounded-[1.75rem] border border-white/45 bg-white/72 shadow-[0_28px_90px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <div className="max-w-md rounded-[1.75rem] border border-white/70 bg-white/82 px-8 py-10 text-center shadow-lg">
              <p className="text-lg font-semibold text-slate-900">Select a conversation</p>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                Choose a seller thread from the left to read messages, ask questions, and continue negotiating.
              </p>
            </div>
          </div>
        )}
      </div>
      </div>
    </div>
  )
}
