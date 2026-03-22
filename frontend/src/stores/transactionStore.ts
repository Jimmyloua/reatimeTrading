import { create } from 'zustand'

type TransactionTab = 'purchases' | 'sales'
type TransactionFilter = 'all' | 'active' | 'completed' | 'cancelled'

interface TransactionState {
  activeTab: TransactionTab
  filter: TransactionFilter
  setTab: (tab: TransactionTab) => void
  setFilter: (filter: TransactionFilter) => void
}

export const useTransactionStore = create<TransactionState>((set) => ({
  activeTab: 'purchases',
  filter: 'all',
  setTab: (tab) => set({ activeTab: tab }),
  setFilter: (filter) => set({ filter }),
}))