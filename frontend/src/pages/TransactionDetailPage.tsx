import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { TransactionStatusBadge } from '@/components/transaction/TransactionStatusBadge'
import { TransactionTimeline } from '@/components/transaction/TransactionTimeline'
import { TransactionActionPanel } from '@/components/transaction/TransactionActionPanel'
import { transactionApi } from '@/api/transactionApi'
import { ArrowLeft } from 'lucide-react'

export function TransactionDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: transaction, isLoading, error } = useQuery({
    queryKey: ['transaction', id],
    queryFn: () => transactionApi.getTransaction(Number(id)),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="container max-w-4xl mx-auto px-4 py-8">
        <Skeleton className="h-8 w-48 mb-6" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Skeleton className="h-64" />
          <Skeleton className="h-64" />
        </div>
      </div>
    )
  }

  if (error || !transaction) {
    return (
      <div className="container max-w-4xl mx-auto px-4 py-8">
        <p className="text-zinc-500">
          Transaction not found. It may have been deleted or you may not have access.
        </p>
        <Button className="mt-4" onClick={() => navigate('/transactions')}>
          Go to Transactions
        </Button>
      </div>
    )
  }

  return (
    <div className="container max-w-4xl mx-auto px-4 py-8">
      <Button variant="ghost" className="mb-4" onClick={() => navigate('/transactions')}>
        <ArrowLeft className="w-4 h-4 mr-2" />
        Back to Transactions
      </Button>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Transaction #{transaction.id}</h1>
        <TransactionStatusBadge status={transaction.status} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left column: Item and Participants */}
        <div className="space-y-6">
          {/* Item Card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Item</CardTitle>
            </CardHeader>
            <CardContent>
              <Link to={`/listings/${transaction.listingId}`} className="flex gap-4">
                <div className="w-20 h-20 rounded-lg bg-zinc-100 overflow-hidden flex-shrink-0">
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
                <div className="flex-1">
                  <h3 className="font-medium">{transaction.listingTitle}</h3>
                  <p className="text-xl font-bold mt-1">${transaction.amount}</p>
                </div>
              </Link>
            </CardContent>
          </Card>

          {/* Participants Card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Participants</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3">
                <span className="text-sm text-zinc-500 w-12">Buyer:</span>
                <Avatar size="sm">
                  <AvatarImage src={transaction.buyerAvatarUrl ?? undefined} />
                  <AvatarFallback>{transaction.buyerName?.charAt(0) || '?'}</AvatarFallback>
                </Avatar>
                <span className="font-medium">{transaction.buyerName}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-sm text-zinc-500 w-12">Seller:</span>
                <Avatar size="sm">
                  <AvatarImage src={transaction.sellerAvatarUrl ?? undefined} />
                  <AvatarFallback>{transaction.sellerName?.charAt(0) || '?'}</AvatarFallback>
                </Avatar>
                <span className="font-medium">{transaction.sellerName}</span>
              </div>
            </CardContent>
          </Card>

          {/* Actions Card */}
          <Card>
            <CardContent className="pt-6">
              <TransactionActionPanel
                transaction={transaction}
                userRole={transaction.userRole}
              />
            </CardContent>
          </Card>
        </div>

        {/* Right column: Timeline */}
        <Card>
          <CardContent className="pt-6">
            <TransactionTimeline transaction={transaction} />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}