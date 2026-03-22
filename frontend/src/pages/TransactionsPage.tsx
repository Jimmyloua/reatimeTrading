import { useQuery } from '@tanstack/react-query'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { TransactionCard } from '@/components/transaction/TransactionCard'
import { transactionApi } from '@/api/transactionApi'
import { useTransactionStore } from '@/stores/transactionStore'
import type { TransactionStatus } from '@/types/transaction'

const TRANSACTIONS_HERO_BG =
  'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&q=80&w=2000'

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

  const totalCount = transactions?.length || 0
  const filteredCount = filteredTransactions?.length || 0

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8">
      <section
        className="relative overflow-hidden rounded-[2.25rem] border border-white/40 px-6 py-10 shadow-[0_28px_90px_rgba(15,23,42,0.18)]"
        style={{
          backgroundImage: `linear-gradient(110deg, rgba(8, 17, 30, 0.88), rgba(23, 70, 111, 0.7) 52%, rgba(255, 194, 86, 0.16)), url(${TRANSACTIONS_HERO_BG})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      >
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.18),transparent_28%)]" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-2xl">
            <p className="text-sm font-medium uppercase tracking-[0.28em] text-amber-200/85">
              Transaction center
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-white md:text-5xl">My Transactions</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-200 md:text-base">
              Track purchase requests, seller actions, and delivery progress in a cleaner dashboard
              inspired by modern resale marketplace flows.
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:w-[360px] lg:grid-cols-1">
            <div className="rounded-[1.5rem] border border-white/20 bg-white/12 p-4 text-white backdrop-blur-md">
              <div className="text-sm uppercase tracking-[0.18em] text-white/65">Overview</div>
              <div className="mt-3 text-3xl font-semibold">{filteredCount}</div>
              <div className="mt-1 text-sm text-slate-200">
                visible items out of {totalCount} total
              </div>
            </div>
            <div className="rounded-[1.5rem] border border-white/20 bg-slate-950/28 p-4 text-white backdrop-blur-md">
              <div className="text-sm uppercase tracking-[0.18em] text-white/65">Focus</div>
              <div className="mt-3 text-lg font-semibold">
                {activeTab === 'purchases' ? 'Buyer-side progress' : 'Seller-side operations'}
              </div>
            </div>
          </div>
        </div>
      </section>

      <Tabs value={activeTab} onValueChange={(v) => setTab(v as 'purchases' | 'sales')} className="mt-8">
        <TabsList className="mb-5 rounded-full border border-slate-200/80 bg-white/90 p-1 shadow-[0_12px_34px_rgba(15,23,42,0.08)]">
          <TabsTrigger value="purchases">Purchases</TabsTrigger>
          <TabsTrigger value="sales">Sales</TabsTrigger>
        </TabsList>

        <div className="mb-5 flex flex-wrap gap-2">
          {['all', 'active', 'completed', 'cancelled'].map((f) => (
            <Button
              key={f}
              variant={filter === f ? 'default' : 'outline'}
              size="sm"
              className={filter === f ? 'shadow-lg' : 'bg-white/88'}
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
            <div className="rounded-[1.75rem] border border-white/70 bg-white/88 py-12 text-center shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
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
            <div className="rounded-[1.75rem] border border-white/70 bg-white/88 py-12 text-center shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
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
