import { Bell } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useNotificationStore } from '@/stores/notificationStore'
import { useNotifications } from '@/hooks/useNotifications'
import { NotificationDropdown } from './NotificationDropdown'

export function NotificationBell() {
  const { unreadCount } = useNotificationStore()
  useNotifications()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="relative rounded-full p-2 hover:bg-accent outline-none">
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <Badge
            variant="destructive"
            className="absolute -top-1 -right-1 h-5 w-5 flex items-center justify-center p-0 text-xs"
          >
            {unreadCount > 50 ? '50+' : unreadCount}
          </Badge>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <NotificationDropdown />
      </DropdownMenuContent>
    </DropdownMenu>
  )
}