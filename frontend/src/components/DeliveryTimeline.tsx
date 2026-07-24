import type { DeliveryStatus } from '../api/types'

const STEPS: { status: DeliveryStatus; label: string }[] = [
  { status: 'READY', label: '준비중' },
  { status: 'SHIPPED', label: '발송됨' },
  { status: 'IN_TRANSIT', label: '배송중' },
  { status: 'DELIVERED', label: '배송완료' },
]

const STEP_INDEX: Record<DeliveryStatus, number> = {
  READY: 0,
  SHIPPED: 1,
  IN_TRANSIT: 2,
  DELIVERED: 3,
  RETURN_REQUESTED: 3,
}

interface DeliveryTimelineProps {
  status: DeliveryStatus
}

export function DeliveryTimeline({ status }: DeliveryTimelineProps) {
  const currentIndex = STEP_INDEX[status]

  return (
    <div className="delivery-timeline">
      {STEPS.map((step, index) => (
        <div
          key={step.status}
          className={
            'delivery-timeline__step' +
            (index < currentIndex ? ' delivery-timeline__step--done' : '') +
            (index === currentIndex ? ' delivery-timeline__step--active' : '')
          }
        >
          <span className="delivery-timeline__dot" />
          <span className="delivery-timeline__label">{step.label}</span>
        </div>
      ))}
      {status === 'RETURN_REQUESTED' && (
        <div className="delivery-timeline__step delivery-timeline__step--return">
          <span className="delivery-timeline__dot" />
          <span className="delivery-timeline__label">반품 요청됨</span>
        </div>
      )}
    </div>
  )
}
