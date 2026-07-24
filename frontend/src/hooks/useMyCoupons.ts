import { useEffect, useState } from 'react'
import { getMyCoupons } from '../api/coupons'
import { useAuth } from '../context/AuthContext'
import type { UserCouponResponse } from '../api/types'

// 상품 목록/상세에서 "보유 쿠폰 적용 시 가격"을 보여주기 위해 로그인 사용자의
// 사용 가능한 쿠폰을 한 번 불러와 재사용합니다.
export function useMyCoupons(): UserCouponResponse[] {
  const { user } = useAuth()
  const [coupons, setCoupons] = useState<UserCouponResponse[]>([])

  useEffect(() => {
    if (!user) {
      setCoupons([])
      return
    }
    getMyCoupons(0, 50)
      .then((result) => setCoupons(result.content.filter((c) => c.status === 'AVAILABLE')))
      .catch(() => setCoupons([]))
  }, [user])

  return coupons
}
