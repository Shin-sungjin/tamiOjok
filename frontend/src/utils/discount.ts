import type { UserCouponResponse } from '../api/types'

export interface BestDiscount {
  discountedPrice: number
  discountAmount: number
  percentLabel: string
  couponName: string
}

// 보유 쿠폰 중 해당 가격에 적용 가능한 것들 중 할인액이 가장 큰 쿠폰을 찾습니다.
export function calculateBestDiscount(price: number, coupons: UserCouponResponse[]): BestDiscount | null {
  const applicable = coupons.filter((c) => c.status === 'AVAILABLE' && price >= c.minOrderAmount)
  if (applicable.length === 0 || price <= 0) {
    return null
  }

  let best: BestDiscount | null = null
  for (const coupon of applicable) {
    let discount =
      coupon.discountType === 'PERCENTAGE'
        ? Math.floor((price * coupon.discountValue) / 100)
        : coupon.discountValue
    if (coupon.maxDiscountAmount != null) {
      discount = Math.min(discount, coupon.maxDiscountAmount)
    }
    discount = Math.min(discount, price)

    if (discount > 0 && (!best || discount > best.discountAmount)) {
      best = {
        discountedPrice: price - discount,
        discountAmount: discount,
        percentLabel: `${Math.round((discount / price) * 100)}%`,
        couponName: coupon.name,
      }
    }
  }

  return best
}
