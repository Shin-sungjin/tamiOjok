import { Link } from 'react-router-dom'

interface EmptyStateProps {
  icon?: string
  message: string
  actionLabel?: string
  actionTo?: string
}

export function EmptyState({ icon = '🗒️', message, actionLabel, actionTo }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon" aria-hidden="true">
        {icon}
      </span>
      <p className="empty-state__message">{message}</p>
      {actionLabel && actionTo && (
        <Link to={actionTo} className="empty-state__action">
          {actionLabel}
        </Link>
      )}
    </div>
  )
}
