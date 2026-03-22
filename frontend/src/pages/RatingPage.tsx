import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { RatingForm } from '@/components/rating/RatingForm'
import { transactionApi } from '@/api/transactionApi'

export function RatingPage() {
  const { transactionId } = useParams<{ transactionId: string }>()

  const { data: transaction, isLoading } = useQuery({
    queryKey: ['transaction', transactionId],
    queryFn: () => transactionApi.getTransaction(Number(transactionId)),
    enabled: !!transactionId,
  })

  if (isLoading) {
    return (
      <div className="container max-w-lg mx-auto px-4 py-8">
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (!transaction || !transaction.canRate) {
    return (
      <div className="container max-w-lg mx-auto px-4 py-8">
        <Card>
          <CardContent className="pt-6 text-center text-zinc-500">
            This transaction is not eligible for rating.
          </CardContent>
        </Card>
      </div>
    )
  }

  const otherPartyName =
    transaction.userRole === 'BUYER' ? transaction.sellerName : transaction.buyerName

  return (
    <div className="container max-w-lg mx-auto px-4 py-8">
      <Card>
        <CardContent className="pt-6">
          <RatingForm
            transactionId={transaction.id}
            otherPartyName={otherPartyName}
            userRole={transaction.userRole}
          />
        </CardContent>
      </Card>
    </div>
  )
}