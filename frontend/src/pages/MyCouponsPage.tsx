import { useEffect, useState } from 'react'
import * as couponsApi from '../api/coupons'
import { extractErrorMessage } from '../api/errors'
import type { UserCouponResponse } from '../api/types'

export function MyCouponsPage() {
  const [coupons, setCoupons] = useState<UserCouponResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    couponsApi
      .getMyCoupons()
      .then((result) => setCoupons(result.content))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [])

  return (
    <div className="page">
      <h1>내 쿠폰함</h1>
      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && coupons.length === 0 && <p>보유한 쿠폰이 없습니다.</p>}

      <ul className="list">
        {coupons.map((coupon) => (
          <li key={coupon.id} className="card">
            <h3>{coupon.name}</h3>
            <p>
              {coupon.discountType === 'PERCENTAGE'
                ? `${coupon.discountValue}% 할인`
                : `${coupon.discountValue.toLocaleString()}원 할인`}
            </p>
            <p className="card__meta">
              <span className="badge">{coupon.status === 'AVAILABLE' ? '사용 가능' : '사용됨'}</span>
              {' · 발급일 '}
              {new Date(coupon.issuedAt).toLocaleDateString()}
            </p>
          </li>
        ))}
      </ul>
    </div>
  )
}
