import { Link } from 'react-router-dom'
import { Card } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { TransactionStatusBadge } from './TransactionStatusBadge'
import type { Transaction } from '@/types/transaction'
import { formatDistanceToNow } from 'date-fns'

interface TransactionCardProps {
  transaction: Transaction
}

export function TransactionCard({ transaction }: TransactionCardProps) {
  const otherParty = transaction.userRole === 'BUYER'
    ? { id: transaction.sellerId, name: transaction.sellerName }
    : { id: transaction.buyerId, name: transaction.buyerName }

  return (
    <Link to={`/transactions/${transaction.id}`}>
      <Card className="cursor-pointer border-white/60 bg-white/92 p-4 shadow-[0_16px_44px_rgba(15,23,42,0.08)] backdrop-blur-sm transition duration-300 hover:-translate-y-1 hover:bg-white hover:shadow-[0_24px_60px_rgba(15,23,42,0.14)]">
        <div className="flex items-start gap-4">
          {/* Item image */}
          <div className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-2xl bg-zinc-100 shadow-inner">
            {transaction.listingImageUrl ? (
              <img
                src={transaction.listingImageUrl}
                alt={transaction.listingTitle}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-zinc-400">
                No image
              </div>
            )}
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2">
              <h3 className="truncate text-base font-semibold text-slate-900">{transaction.listingTitle}</h3>
              <TransactionStatusBadge status={transaction.status} />
            </div>

            <div className="mt-2 flex items-center justify-between gap-3">
              <p className="text-xl font-semibold text-slate-950">${transaction.amount}</p>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">
                {transaction.userRole === 'BUYER' ? 'Purchase' : 'Sale'}
              </span>
            </div>

            <div className="mt-3 flex items-center gap-2 text-sm text-zinc-500">
              <span>{transaction.userRole === 'BUYER' ? 'Seller' : 'Buyer'}:</span>
              <Avatar size="sm">
                <AvatarFallback>{otherParty.name?.charAt(0) || '?'}</AvatarFallback>
              </Avatar>
              <span>{otherParty.name}</span>
            </div>

            <p className="mt-2 text-xs uppercase tracking-[0.14em] text-zinc-400">
              {formatDistanceToNow(new Date(transaction.createdAt), { addSuffix: true })}
            </p>
          </div>
        </div>
      </Card>
    </Link>
  )
}
