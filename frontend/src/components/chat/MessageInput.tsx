import { useState, useRef } from 'react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Send, Image as ImageIcon } from 'lucide-react'

interface MessageInputProps {
  onSend: (content: string, imageUrl?: string) => void | Promise<void>
  onTyping: () => void
  canSend?: boolean
  statusMessage?: string | null
}

export function MessageInput({ onSend, onTyping, canSend = true, statusMessage }: MessageInputProps) {
  const [message, setMessage] = useState('')
  const [imageUrl, setImageUrl] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const handleSubmit = () => {
    if ((!message.trim() && !imageUrl) || !canSend) return

    onSend(message.trim(), imageUrl || undefined)
    setMessage('')
    setImageUrl(null)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setMessage(e.target.value)
    onTyping()
  }

  return (
    <div className="sticky bottom-0 border-t border-slate-200/70 bg-white/80 px-4 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))] backdrop-blur-xl md:pb-4">
      {imageUrl && (
        <div className="mb-2 relative inline-block">
          <img src={imageUrl} alt="Preview" className="max-h-32 rounded-lg" />
          <button
            onClick={() => setImageUrl(null)}
            className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1 text-xs"
          >
            x
          </button>
        </div>
      )}
      {statusMessage ? (
        <p className="mb-3 text-xs font-medium text-slate-500">{statusMessage}</p>
      ) : null}
      <div className="flex items-end gap-2">
        <Button variant="ghost" size="icon" disabled={!canSend}>
          <ImageIcon className="h-5 w-5" />
        </Button>
        <Textarea
          ref={textareaRef}
          autoFocus
          value={message}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder="Type a message..."
          className="min-h-[40px] max-h-32 resize-none border-slate-200 bg-white/90 shadow-sm"
          rows={1}
        />
        <Button
          onClick={handleSubmit}
          disabled={!canSend || (!message.trim() && !imageUrl)}
          className="bg-slate-900 text-white shadow-lg shadow-slate-900/20 hover:bg-slate-800"
        >
          <Send className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}
