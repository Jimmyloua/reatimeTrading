import { useState, useRef } from 'react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Send, Image as ImageIcon } from 'lucide-react'

interface MessageInputProps {
  onSend: (content: string, imageUrl?: string) => void
  onTyping: () => void
  disabled?: boolean
}

export function MessageInput({ onSend, onTyping, disabled }: MessageInputProps) {
  const [message, setMessage] = useState('')
  const [imageUrl, setImageUrl] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const handleSubmit = () => {
    if ((!message.trim() && !imageUrl) || disabled) return

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
    <div className="border-t p-4">
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
      <div className="flex items-end gap-2">
        <Button variant="ghost" size="icon" disabled={disabled}>
          <ImageIcon className="h-5 w-5" />
        </Button>
        <Textarea
          ref={textareaRef}
          value={message}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder="Type a message..."
          className="min-h-[40px] max-h-32 resize-none"
          rows={1}
          disabled={disabled}
        />
        <Button onClick={handleSubmit} disabled={disabled || (!message.trim() && !imageUrl)}>
          <Send className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}