import { calculateBestDiscount } from '../utils/discount'
import type { UserCouponResponse } from '../api/types'

interface PriceDisplayProps {
  price: number
  coupons: UserCouponResponse[]
  size?: 'sm' | 'md'
}

export function PriceDisplay({ price, coupons, size = 'md' }: PriceDisplayProps) {
  const best = calculateBestDiscount(price, coupons)

  if (!best) {
    return (
      <p className={`price-display price-display--${size}`}>
        <span className="price-display__final">{price.toLocaleString()}원</span>
      </p>
    )
  }

  return (
    <p className={`price-display price-display--${size}`}>
      <span className="price-display__original">{price.toLocaleString()}원</span>
      <span className="price-display__row">
        <span className="price-display__percent">{best.percentLabel}</span>
        <span className="price-display__final">{best.discountedPrice.toLocaleString()}원</span>
      </span>
      <span className="price-display__coupon">{best.couponName} 적용</span>
    </p>
  )
}
