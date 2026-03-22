import { useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { transactionApi } from '@/api/transactionApi'
import { toast } from 'sonner'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'

interface RequestToBuyButtonProps {
  listingId: number
  sellerId: number
  conversationId?: number
  isOwner: boolean
}

export function RequestToBuyButton({
  listingId,
  sellerId,
  conversationId,
  isOwner,
}: RequestToBuyButtonProps) {
  const [showConfirmDialog, setShowConfirmDialog] = useState(false)
  const navigate = useNavigate()

  const createMutation = useMutation({
    mutationFn: () =>
      transactionApi.createTransaction({
        listingId,
        conversationId,
        idempotencyKey: `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`,
      }),
    onSuccess: (transaction) => {
      toast.success('Purchase request sent. The seller will respond shortly.')
      setShowConfirmDialog(false)
      navigate(`/transactions/${transaction.id}`)
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Failed to create request'
      toast.error(message)
    },
  })

  // Don't show for own listings
  if (isOwner) {
    return null
  }

  return (
    <>
      <Button
        onClick={() => setShowConfirmDialog(true)}
        className="w-full"
        size="lg"
      >
        Request to Buy
      </Button>

      <Dialog open={showConfirmDialog} onOpenChange={setShowConfirmDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Request to Buy</DialogTitle>
            <DialogDescription>
              This will send a purchase request to the seller. Once accepted, the transaction
              will begin and the item will be reserved for you.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowConfirmDialog(false)}>
              Cancel
            </Button>
            <Button
              onClick={() => createMutation.mutate()}
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? 'Sending...' : 'Send Request'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}