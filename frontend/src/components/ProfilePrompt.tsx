import { useState } from 'react'
import { useAuthStore } from '@/stores/authStore'
import { userApi } from '@/api/userApi'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { toast } from 'sonner'

interface ProfilePromptProps {
  isOpen: boolean
  onComplete: () => void
  onClose: () => void
}

/**
 * Profile completion prompt component.
 *
 * IMPORTANT: This component is created in Phase 1 but will NOT be triggered until Phase 2.
 * D-06 and D-07 apply to creating listings and starting chats, which don't exist in Phase 1.
 * The component exists for Phase 2 integration when those features are built.
 */
export function ProfilePrompt({ isOpen, onComplete, onClose }: ProfilePromptProps) {
  const { user, setUser } = useAuthStore()
  const [displayName, setDisplayName] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [validationError, setValidationError] = useState<string | null>(null)

  // Handle display name change
  const handleDisplayNameChange = (value: string) => {
    setDisplayName(value)
    // Validate: max 50 characters (UI-SPEC.md)
    if (value.length > 50) {
      setValidationError('Display name must be 50 characters or less')
    } else {
      setValidationError(null)
    }
  }

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    // Validate display name is not empty
    if (!displayName.trim()) {
      setValidationError('Display name is required')
      return
    }

    // Validate before submission
    if (displayName.length > 50) {
      setValidationError('Display name must be 50 characters or less')
      return
    }

    setIsLoading(true)
    try {
      const updatedUser = await userApi.updateProfile({
        displayName: displayName.trim(),
      })

      // Update auth store with updated user
      if (user) {
        setUser({ ...user, displayName: updatedUser.displayName })
      }

      toast.success('Profile updated successfully')
      onComplete()
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : 'Failed to update profile'
      )
    } finally {
      setIsLoading(false)
    }
  }

  // Handle dialog close
  const handleOpenChange = (open: boolean) => {
    if (!open) {
      onClose()
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Set up your profile</DialogTitle>
          <DialogDescription>
            Add a display name to help others know who you are. This is required
            before you can create listings or start chats.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="prompt-displayName">Display Name</Label>
            <Input
              id="prompt-displayName"
              type="text"
              placeholder="Enter your display name"
              value={displayName}
              onChange={(e) => handleDisplayNameChange(e.target.value)}
              maxLength={50}
              autoFocus
            />
            {validationError && (
              <p className="text-sm text-destructive">{validationError}</p>
            )}
          </div>

          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading || !!validationError}>
              {isLoading ? 'Saving...' : 'Save'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}