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
      <Card className="p-4 hover:bg-zinc-50 transition-colors cursor-pointer">
        <div className="flex items-start gap-4">
          {/* Item image */}
          <div className="w-16 h-16 rounded-lg bg-zinc-100 overflow-hidden flex-shrink-0">
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
              <h3 className="font-medium truncate">{transaction.listingTitle}</h3>
              <TransactionStatusBadge status={transaction.status} />
            </div>

            <p className="text-lg font-semibold mt-1">${transaction.amount}</p>

            <div className="flex items-center gap-2 mt-2 text-sm text-zinc-500">
              <span>{transaction.userRole === 'BUYER' ? 'Seller' : 'Buyer'}:</span>
              <Avatar size="sm">
                <AvatarFallback>{otherParty.name?.charAt(0) || '?'}</AvatarFallback>
              </Avatar>
              <span>{otherParty.name}</span>
            </div>

            <p className="text-xs text-zinc-400 mt-1">
              {formatDistanceToNow(new Date(transaction.createdAt), { addSuffix: true })}
            </p>
          </div>
        </div>
      </Card>
    </Link>
  )
}