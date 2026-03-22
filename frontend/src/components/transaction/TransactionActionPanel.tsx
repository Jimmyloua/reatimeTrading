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
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import type { TransactionDetail } from '@/types/transaction'
import { transactionApi } from '@/api/transactionApi'
import { toast } from 'sonner'
import { useMutation, useQueryClient } from '@tanstack/react-query'

interface TransactionActionPanelProps {
  transaction: TransactionDetail
  userRole: 'BUYER' | 'SELLER'
}

export function TransactionActionPanel({ transaction, userRole }: TransactionActionPanelProps) {
  const [showCancelDialog, setShowCancelDialog] = useState(false)
  const [showDeclineDialog, setShowDeclineDialog] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const queryClient = useQueryClient()

  // Mutations
  const acceptMutation = useMutation({
    mutationFn: () => transactionApi.acceptRequest(transaction.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transaction', transaction.id] })
      toast.success('Request accepted')
    },
    onError: () => toast.error('Failed to accept request'),
  })

  const declineMutation = useMutation({
    mutationFn: (reason: string) => transactionApi.declineRequest(transaction.id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transaction', transaction.id] })
      toast.success('Request declined')
      setShowDeclineDialog(false)
    },
    onError: () => toast.error('Failed to decline request'),
  })

  const confirmPaymentMutation = useMutation({
    mutationFn: () => transactionApi.confirmPayment(transaction.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transaction', transaction.id] })
      toast.success('Payment confirmed')
    },
    onError: () => toast.error('Failed to confirm payment'),
  })

  const confirmFundsMutation = useMutation({
    mutationFn: () => transactionApi.confirmFunds(transaction.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transaction', transaction.id] })
      toast.success('Funds confirmed')
    },
    onError: () => toast.error('Failed to confirm funds'),
  })

  const markDeliveredMutation = useMutation({
    mutationFn: () => transactionApi.markDelivered(transaction.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transaction', transaction.id] })
      toast.success('Marked as shipped')
    },
    onError: () => toast.error('Failed to mark as shipped'),
  })

  const confirmReceiptMutation = useMutation({
    mutationFn: () => transactionApi.confirmReceipt(transaction.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transaction', transaction.id] })
      toast.success('Receipt confirmed')
    },
    onError: () => toast.error('Failed to confirm receipt'),
  })

  const cancelMutation = useMutation({
    mutationFn: (reason: string) => transactionApi.cancelTransaction(transaction.id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transaction', transaction.id] })
      toast.success('Transaction cancelled')
      setShowCancelDialog(false)
    },
    onError: () => toast.error('Failed to cancel transaction'),
  })

  return (
    <>
      <div className="space-y-3">
        {/* Seller actions */}
        {userRole === 'SELLER' && transaction.canCancel && (
          <div className="flex gap-2">
            <Button
              onClick={() => acceptMutation.mutate()}
              disabled={acceptMutation.isPending}
              className="flex-1"
            >
              Accept Request
            </Button>
            <Button
              variant="destructive"
              onClick={() => setShowDeclineDialog(true)}
              disabled={declineMutation.isPending}
            >
              Decline
            </Button>
          </div>
        )}

        {/* Buyer actions */}
        {userRole === 'BUYER' && transaction.canConfirmPayment && (
          <Button
            onClick={() => confirmPaymentMutation.mutate()}
            disabled={confirmPaymentMutation.isPending}
            className="w-full"
          >
            Confirm Payment Sent
          </Button>
        )}

        {/* Seller actions */}
        {userRole === 'SELLER' && transaction.canConfirmFunds && (
          <Button
            onClick={() => confirmFundsMutation.mutate()}
            disabled={confirmFundsMutation.isPending}
            className="w-full"
          >
            Confirm Funds Received
          </Button>
        )}

        {userRole === 'SELLER' && transaction.canMarkDelivered && (
          <Button
            onClick={() => markDeliveredMutation.mutate()}
            disabled={markDeliveredMutation.isPending}
            className="w-full"
          >
            Mark as Shipped
          </Button>
        )}

        {/* Buyer actions */}
        {userRole === 'BUYER' && transaction.canConfirmReceipt && (
          <Button
            onClick={() => confirmReceiptMutation.mutate()}
            disabled={confirmReceiptMutation.isPending}
            className="w-full"
          >
            Confirm Receipt
          </Button>
        )}

        {/* Cancel action */}
        {transaction.canCancel && (
          <Button
            variant="outline"
            onClick={() => setShowCancelDialog(true)}
            className="w-full"
          >
            Cancel Transaction
          </Button>
        )}

        {/* Rate action */}
        {transaction.canRate && (
          <Button
            variant="outline"
            onClick={() => {/* Navigate to rating page */}}
            className="w-full"
          >
            Leave a Review
          </Button>
        )}
      </div>

      {/* Cancel Dialog */}
      <Dialog open={showCancelDialog} onOpenChange={setShowCancelDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel Transaction</DialogTitle>
            <DialogDescription>
              Are you sure you want to cancel this transaction? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label htmlFor="reason">Reason (optional)</Label>
              <Textarea
                id="reason"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="Why are you cancelling?"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCancelDialog(false)}>
              Keep Transaction
            </Button>
            <Button
              variant="destructive"
              onClick={() => cancelMutation.mutate(cancelReason)}
              disabled={cancelMutation.isPending}
            >
              Cancel Transaction
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Decline Dialog */}
      <Dialog open={showDeclineDialog} onOpenChange={setShowDeclineDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Decline Request</DialogTitle>
            <DialogDescription>
              The buyer will be notified that their request was declined.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDeclineDialog(false)}>
              Go Back
            </Button>
            <Button
              variant="destructive"
              onClick={() => declineMutation.mutate(cancelReason)}
              disabled={declineMutation.isPending}
            >
              Decline Request
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}