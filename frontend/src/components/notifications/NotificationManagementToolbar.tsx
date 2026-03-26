import type { Notification } from '@/types/notification'

type NotificationManagementTab = 'all' | 'unread'
type ManagedNotificationType = Extract<
  Notification['type'],
  'NEW_MESSAGE' | 'ITEM_SOLD' | 'TRANSACTION_UPDATE'
>

interface NotificationManagementToolbarProps {
  tab: NotificationManagementTab
  types: ManagedNotificationType[]
  hasUnreadVisible: boolean
  onTabChange: (tab: NotificationManagementTab) => void
  onTypeToggle: (type: ManagedNotificationType) => void
  onMarkVisibleAsRead: () => void
}

const typeOptions: Array<{ value: ManagedNotificationType; label: string }> = [
  { value: 'NEW_MESSAGE', label: 'New messages' },
  { value: 'ITEM_SOLD', label: 'Item sold' },
  { value: 'TRANSACTION_UPDATE', label: 'Transaction updates' },
]

export function NotificationManagementToolbar({
  tab,
  types,
  hasUnreadVisible,
  onTabChange,
  onTypeToggle,
  onMarkVisibleAsRead,
}: NotificationManagementToolbarProps) {
  return (
    <div className="flex flex-col gap-4 rounded-lg border bg-white p-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="space-y-2">
          <div aria-label="Notification views" className="inline-flex rounded-full bg-slate-100 p-1" role="tablist">
            <button
              aria-selected={tab === 'all'}
              className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                tab === 'all' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600'
              }`}
              onClick={() => onTabChange('all')}
              role="tab"
              type="button"
            >
              All
            </button>
            <button
              aria-selected={tab === 'unread'}
              className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                tab === 'unread' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600'
              }`}
              onClick={() => onTabChange('unread')}
              role="tab"
              type="button"
            >
              Unread
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            {typeOptions.map((option) => {
              const selected = types.includes(option.value)

              return (
                <button
                  aria-pressed={selected}
                  className={`rounded-full border px-3 py-1.5 text-sm transition ${
                    selected
                      ? 'border-slate-900 bg-slate-900 text-white'
                      : 'border-slate-200 bg-white text-slate-600'
                  }`}
                  key={option.value}
                  onClick={() => onTypeToggle(option.value)}
                  type="button"
                >
                  {option.label}
                </button>
              )
            })}
          </div>
        </div>

        <button
          className="rounded-full border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-300 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={!hasUnreadVisible}
          onClick={onMarkVisibleAsRead}
          type="button"
        >
          Mark visible as read
        </button>
      </div>
    </div>
  )
}
