import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as cartApi from '../api/cart'
import * as couponsApi from '../api/coupons'
import * as ordersApi from '../api/orders'
import { extractErrorMessage } from '../api/errors'
import { useCart } from '../context/CartContext'
import type { CartResponse, UserCouponResponse } from '../api/types'

export function CheckoutPage() {
  const navigate = useNavigate()
  const { refreshCart } = useCart()
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [coupons, setCoupons] = useState<UserCouponResponse[]>([])
  const [selectedCouponId, setSelectedCouponId] = useState<string>('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    Promise.all([cartApi.getMyCart(), couponsApi.getMyCoupons(0, 50)])
      .then(([cartResult, couponResult]) => {
        setCart(cartResult)
        setCoupons(couponResult.content.filter((c) => c.status === 'AVAILABLE'))
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [])

  async function handlePlaceOrder() {
    setError(null)
    setIsSubmitting(true)
    try {
      const userCouponId = selectedCouponId ? Number(selectedCouponId) : undefined
      const order = await ordersApi.createOrderFromCart(userCouponId)
      await refreshCart()
      navigate(`/orders/${order.id}`)
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <p className="page">불러오는 중...</p>
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="page">
        <h1>주문서</h1>
        <p>장바구니가 비어있어 주문할 수 없습니다.</p>
      </div>
    )
  }

  return (
    <div className="page">
      <h1>주문서</h1>
      <div className="table-scroll">
      <table className="table">
        <thead>
          <tr>
            <th>상품</th>
            <th>수량</th>
            <th>합계</th>
          </tr>
        </thead>
        <tbody>
          {cart.items.map((item) => (
            <tr key={item.id}>
              <td>{item.productName}</td>
              <td>{item.quantity}</td>
              <td>{item.lineTotal.toLocaleString()}원</td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>

      <label className="checkout-coupon">
        보유 쿠폰 적용
        <select value={selectedCouponId} onChange={(e) => setSelectedCouponId(e.target.value)}>
          <option value="">사용 안 함</option>
          {coupons.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name} ({c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : `${c.discountValue.toLocaleString()}원`})
            </option>
          ))}
        </select>
      </label>

      <div className="cart-summary">
        <span>상품 합계: {cart.totalAmount.toLocaleString()}원</span>
        <button type="button" onClick={handlePlaceOrder} disabled={isSubmitting}>
          {isSubmitting ? '주문 생성 중...' : '결제하기'}
        </button>
      </div>
      {error && <p className="form-error">{error}</p>}
    </div>
  )
}
