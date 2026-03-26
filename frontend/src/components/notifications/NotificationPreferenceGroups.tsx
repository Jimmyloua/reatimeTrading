import type { NotificationPreferences } from '@/types/notification'

interface NotificationPreferenceGroupsProps {
  preferences: NotificationPreferences
  onPreferenceChange: (key: keyof NotificationPreferences, value: boolean) => void | Promise<void>
}

const preferenceGroups: Array<{
  label: string
  items: Array<{
    key: keyof NotificationPreferences
    label: string
  }>
}> = [
  {
    label: 'Messages',
    items: [
      {
        key: 'newMessageEnabled',
        label: 'New messages',
      },
    ],
  },
  {
    label: 'Sales',
    items: [
      {
        key: 'itemSoldEnabled',
        label: 'Item sold',
      },
    ],
  },
  {
    label: 'Transactions',
    items: [
      {
        key: 'transactionUpdateEnabled',
        label: 'Transaction updates',
      },
    ],
  },
]

export function NotificationPreferenceGroups({
  preferences,
  onPreferenceChange,
}: NotificationPreferenceGroupsProps) {
  return (
    <div className="mt-4 space-y-4">
      {preferenceGroups.map((group) => (
        <div className="space-y-3" key={group.label}>
          <h3 className="text-xs font-semibold uppercase tracking-wide text-neutral-500">
            {group.label}
          </h3>
          {group.items.map((item) => (
            <label className="flex items-center justify-between gap-3 text-sm" key={item.key}>
              <span>{item.label}</span>
              <input
                type="checkbox"
                aria-label={item.label}
                checked={preferences[item.key]}
                onChange={(event) => {
                  void onPreferenceChange(item.key, event.target.checked)
                }}
              />
            </label>
          ))}
        </div>
      ))}
    </div>
  )
}
