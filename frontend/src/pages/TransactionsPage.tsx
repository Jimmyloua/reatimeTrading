import { useQuery } from '@tanstack/react-query'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { TransactionCard } from '@/components/transaction/TransactionCard'
import { transactionApi } from '@/api/transactionApi'
import { useTransactionStore } from '@/stores/transactionStore'
import type { TransactionStatus } from '@/types/transaction'

// Filter statuses for each filter type
const FILTER_STATUSES: Record<string, TransactionStatus[]> = {
  all: [],
  active: ['CREATED', 'FUNDED', 'RESERVED', 'DELIVERED', 'CONFIRMED', 'SETTLED'],
  completed: ['COMPLETED'],
  cancelled: ['CANCELLED', 'EXPIRED'],
}

export function TransactionsPage() {
  const { activeTab, filter, setTab, setFilter } = useTransactionStore()

  const { data: purchases, isLoading: loadingPurchases } = useQuery({
    queryKey: ['transactions', 'purchases'],
    queryFn: () => transactionApi.getPurchases(),
    enabled: activeTab === 'purchases',
  })

  const { data: sales, isLoading: loadingSales } = useQuery({
    queryKey: ['transactions', 'sales'],
    queryFn: () => transactionApi.getSales(),
    enabled: activeTab === 'sales',
  })

  const transactions = activeTab === 'purchases' ? purchases?.content : sales?.content
  const isLoading = activeTab === 'purchases' ? loadingPurchases : loadingSales

  // Apply filter
  const filteredTransactions = transactions?.filter((t) => {
    if (filter === 'all') return true
    return FILTER_STATUSES[filter]?.includes(t.status)
  })

  return (
    <div className="container max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">My Transactions</h1>

      <Tabs value={activeTab} onValueChange={(v) => setTab(v as 'purchases' | 'sales')}>
        <TabsList className="mb-4">
          <TabsTrigger value="purchases">Purchases</TabsTrigger>
          <TabsTrigger value="sales">Sales</TabsTrigger>
        </TabsList>

        <div className="flex gap-2 mb-4">
          {['all', 'active', 'completed', 'cancelled'].map((f) => (
            <Button
              key={f}
              variant={filter === f ? 'default' : 'outline'}
              size="sm"
              onClick={() => setFilter(f as 'all' | 'active' | 'completed' | 'cancelled')}
            >
              {f.charAt(0).toUpperCase() + f.slice(1)}
            </Button>
          ))}
        </div>

        <TabsContent value="purchases">
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <Skeleton key={i} className="h-24 w-full" />
              ))}
            </div>
          ) : filteredTransactions?.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-zinc-500 mb-2">
                {filter === 'all'
                  ? 'No purchases yet'
                  : 'No transactions match this filter'}
              </p>
              <p className="text-sm text-zinc-400">
                {filter === 'all' && 'When you buy items on the marketplace, your transactions will appear here.'}
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredTransactions?.map((transaction) => (
                <TransactionCard key={transaction.id} transaction={transaction} />
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="sales">
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <Skeleton key={i} className="h-24 w-full" />
              ))}
            </div>
          ) : filteredTransactions?.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-zinc-500 mb-2">
                {filter === 'all'
                  ? 'No sales yet'
                  : 'No transactions match this filter'}
              </p>
              <p className="text-sm text-zinc-400">
                {filter === 'all' && 'When buyers request to purchase your items, their requests will appear here.'}
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredTransactions?.map((transaction) => (
                <TransactionCard key={transaction.id} transaction={transaction} />
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}