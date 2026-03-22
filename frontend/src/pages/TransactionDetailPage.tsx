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

const TRANSACTION_DETAIL_BG =
  'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&q=80&w=2000'

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
    <div className="container mx-auto max-w-5xl px-4 py-8">
      <Button variant="ghost" className="mb-4 bg-white/70" onClick={() => navigate('/transactions')}>
        <ArrowLeft className="w-4 h-4 mr-2" />
        Back to Transactions
      </Button>

      <section
        className="relative mb-6 overflow-hidden rounded-[2.25rem] border border-white/40 px-6 py-8 shadow-[0_28px_90px_rgba(15,23,42,0.18)]"
        style={{
          backgroundImage: `linear-gradient(110deg, rgba(8, 17, 30, 0.88), rgba(23, 70, 111, 0.7) 52%, rgba(255, 194, 86, 0.16)), url(${TRANSACTION_DETAIL_BG})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      >
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.18),transparent_28%)]" />
        <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-medium uppercase tracking-[0.28em] text-amber-200/85">
              Transaction detail
            </p>
            <h1 className="mt-3 text-3xl font-semibold text-white md:text-4xl">
              Transaction #{transaction.id}
            </h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-200 md:text-base">
              Review item value, participants, current status, and every milestone in the order
              flow from one focused page.
            </p>
          </div>
          <div className="rounded-[1.5rem] border border-white/20 bg-white/12 p-4 text-white backdrop-blur-md">
            <div className="text-sm uppercase tracking-[0.18em] text-white/65">Current status</div>
            <div className="mt-3">
              <TransactionStatusBadge status={transaction.status} />
            </div>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left column: Item and Participants */}
        <div className="space-y-6">
          {/* Item Card */}
          <Card className="border-white/60 bg-white/92 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
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
          <Card className="border-white/60 bg-white/92 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
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
          <Card className="border-white/60 bg-white/92 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
            <CardContent className="pt-6">
              <TransactionActionPanel
                transaction={transaction}
                userRole={transaction.userRole}
              />
            </CardContent>
          </Card>
        </div>

        {/* Right column: Timeline */}
        <Card className="border-white/60 bg-white/92 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
          <CardContent className="pt-6">
            <TransactionTimeline transaction={transaction} />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
