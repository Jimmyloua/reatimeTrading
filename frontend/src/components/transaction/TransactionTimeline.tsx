import { Check, Clock } from 'lucide-react'
import type { TransactionDetail, TransactionStatus } from '@/types/transaction'

interface TransactionTimelineProps {
  transaction: TransactionDetail
}

interface TimelineStep {
  status: TransactionStatus
  timestamp: string | null
  label: string
}

export function TransactionTimeline({ transaction }: TransactionTimelineProps) {
  const steps: TimelineStep[] = [
    { status: 'CREATED', timestamp: transaction.createdAt, label: 'Request Created' },
    { status: 'FUNDED', timestamp: transaction.fundedAt, label: 'Payment Sent' },
    { status: 'RESERVED', timestamp: transaction.reservedAt, label: 'Funds Received' },
    { status: 'DELIVERED', timestamp: transaction.deliveredAt, label: 'Shipped' },
    { status: 'CONFIRMED', timestamp: transaction.confirmedAt, label: 'Delivered' },
    { status: 'SETTLED', timestamp: transaction.settledAt, label: 'Settled' },
    { status: 'COMPLETED', timestamp: transaction.completedAt, label: 'Completed' },
  ]

  // Find current step index
  const currentIndex = steps.findIndex(
    (step) => step.status === transaction.status ||
               (step.timestamp === null && steps.indexOf(step) > steps.findIndex(s => s.timestamp === null))
  )

  return (
    <div className="space-y-4">
      <h3 className="font-semibold">Transaction Progress</h3>
      <div className="relative">
        {steps.map((step, index) => {
          const isCompleted = step.timestamp !== null
          const isCurrent = step.status === transaction.status
          const isPending = !isCompleted && index > currentIndex

          return (
            <div key={step.status} className="flex items-start gap-3 pb-4 last:pb-0">
              {/* Icon */}
              <div
                className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 ${
                  isCompleted
                    ? 'bg-green-500 text-white'
                    : isCurrent
                    ? 'bg-blue-500 text-white animate-pulse'
                    : 'bg-zinc-200 text-zinc-400'
                }`}
              >
                {isCompleted ? (
                  <Check className="w-4 h-4" />
                ) : (
                  <Clock className="w-4 h-4" />
                )}
              </div>

              {/* Connector line */}
              {index < steps.length - 1 && (
                <div
                  className={`absolute left-3 w-0.5 h-8 ${
                    isCompleted ? 'bg-green-500' : 'bg-zinc-200'
                  }`}
                  style={{ top: `${index * 52 + 24}px` }}
                />
              )}

              {/* Label */}
              <div className="flex-1">
                <p className={`font-medium ${isCurrent ? 'text-blue-600' : isCompleted ? 'text-zinc-900' : 'text-zinc-400'}`}>
                  {step.label}
                </p>
                {step.timestamp && (
                  <p className="text-xs text-zinc-500">
                    {new Date(step.timestamp).toLocaleString()}
                  </p>
                )}
                {!step.timestamp && !isPending && (
                  <p className="text-xs text-zinc-400">Pending</p>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}