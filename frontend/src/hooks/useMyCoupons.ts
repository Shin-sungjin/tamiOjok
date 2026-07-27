import { useEffect, useState } from 'react'
import { getMyCoupons } from '../api/coupons'
import { useAuth } from '../context/AuthContext'
import type { UserCouponResponse } from '../api/types'

const CACHE_TTL_MS = 60_000

// 상품목록/찜목록/상품상세 페이지를 오갈 때마다 "보유 쿠폰 적용 시 가격"
// 표시를 위해 이 훅이 매번 새로 마운트되면서 동일한 쿠폰 목록을 반복
// 요청하고 있었음. 모듈 스코프에 짧게(60초) 캐시해서 빠른 페이지 전환 중
// 중복 요청을 줄이되, 체크아웃처럼 정확도가 중요한 화면은 이 훅을 쓰지 않고
// 항상 직접 새로 조회하므로 캐시 지연이 실제 결제 흐름에 영향을 주지 않음.
let cachedUserId: number | null = null
let cachedAt = 0
let cachedCoupons: UserCouponResponse[] = []
let inFlight: Promise<UserCouponResponse[]> | null = null

async function fetchAvailableCoupons(): Promise<UserCouponResponse[]> {
  const result = await getMyCoupons(0, 50)
  return result.content.filter((c) => c.status === 'AVAILABLE')
}

export function useMyCoupons(): UserCouponResponse[] {
  const { user } = useAuth()
  const isCacheFresh = user != null && user.id === cachedUserId && Date.now() - cachedAt < CACHE_TTL_MS
  const [coupons, setCoupons] = useState<UserCouponResponse[]>(isCacheFresh ? cachedCoupons : [])

  useEffect(() => {
    if (!user) {
      cachedUserId = null
      cachedCoupons = []
      setCoupons([])
      return
    }

    if (user.id === cachedUserId && Date.now() - cachedAt < CACHE_TTL_MS) {
      setCoupons(cachedCoupons)
      return
    }

    inFlight ??= fetchAvailableCoupons()
    inFlight
      .then((result) => {
        cachedUserId = user.id
        cachedAt = Date.now()
        cachedCoupons = result
        setCoupons(result)
      })
      .catch(() => setCoupons([]))
      .finally(() => {
        inFlight = null
      })
  }, [user])

  return coupons
}
