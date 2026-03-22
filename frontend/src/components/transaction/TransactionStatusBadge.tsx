import { Badge } from '@/components/ui/badge'
import type { TransactionStatus } from '@/types/transaction'
import { TRANSACTION_STATUS_COLORS, TRANSACTION_STATUS_LABELS } from '@/types/transaction'

interface TransactionStatusBadgeProps {
  status: TransactionStatus
}

export function TransactionStatusBadge({ status }: TransactionStatusBadgeProps) {
  const colorClass = TRANSACTION_STATUS_COLORS[status]
  const label = TRANSACTION_STATUS_LABELS[status]

  return (
    <Badge className={`${colorClass} text-white`}>
      {label}
    </Badge>
  )
}