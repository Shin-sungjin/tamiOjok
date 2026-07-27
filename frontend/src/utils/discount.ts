import type { UserCouponResponse } from '../api/types'

export interface BestDiscount {
  discountedPrice: number
  discountAmount: number
  percentLabel: string
  couponName: string
}

// 특정 쿠폰 하나를 적용했을 때의 할인액을 계산합니다.
// 백엔드 Coupon.calculateDiscount()와 동일한 규칙(정률은 소수점 버림, 최대할인액
// 캡, 상품금액 초과 불가)을 따릅니다 — 체크아웃 미리보기와 실제 주문 생성 시
// 서버 계산 결과가 어긋나지 않도록 맞춰둔 것.
export function calculateCouponDiscount(price: number, coupon: UserCouponResponse): number {
  if (price < coupon.minOrderAmount) {
    return 0
  }
  let discount =
    coupon.discountType === 'PERCENTAGE'
      ? Math.floor((price * coupon.discountValue) / 100)
      : coupon.discountValue
  if (coupon.maxDiscountAmount != null) {
    discount = Math.min(discount, coupon.maxDiscountAmount)
  }
  return Math.min(discount, price)
}

// 보유 쿠폰 중 해당 가격에 적용 가능한 것들 중 할인액이 가장 큰 쿠폰을 찾습니다.
export function calculateBestDiscount(price: number, coupons: UserCouponResponse[]): BestDiscount | null {
  const applicable = coupons.filter((c) => c.status === 'AVAILABLE' && price >= c.minOrderAmount)
  if (applicable.length === 0 || price <= 0) {
    return null
  }

  let best: BestDiscount | null = null
  for (const coupon of applicable) {
    const discount = calculateCouponDiscount(price, coupon)

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
