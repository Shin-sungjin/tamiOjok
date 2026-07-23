import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as couponsApi from '../api/coupons'
import { extractErrorMessage } from '../api/errors'
import type { CouponResponse } from '../api/types'

export function CouponListPage() {
  const [coupons, setCoupons] = useState<CouponResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    couponsApi
      .getAvailableCoupons()
      .then((result) => setCoupons(result.content))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [])

  async function handleIssue(couponId: number) {
    setError(null)
    setMessage(null)
    try {
      await couponsApi.issueCoupon(couponId)
      setMessage('쿠폰을 발급받았습니다.')
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>발급 가능한 쿠폰</h1>
        <Link to="/coupons/my">내 쿠폰함</Link>
      </div>
      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {message && <p>{message}</p>}
      {!isLoading && coupons.length === 0 && <p>발급 가능한 쿠폰이 없습니다.</p>}

      <ul className="list">
        {coupons.map((coupon) => (
          <li key={coupon.id} className="card">
            <h3>{coupon.name}</h3>
            <p>
              {coupon.discountType === 'PERCENTAGE'
                ? `${coupon.discountValue}% 할인`
                : `${coupon.discountValue.toLocaleString()}원 할인`}
              {coupon.minOrderAmount > 0 && ` · ${coupon.minOrderAmount.toLocaleString()}원 이상 주문 시`}
            </p>
            <p className="card__meta">
              {new Date(coupon.validFrom).toLocaleDateString()} ~ {new Date(coupon.validUntil).toLocaleDateString()}
            </p>
            <button type="button" onClick={() => handleIssue(coupon.id)}>
              발급받기
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
