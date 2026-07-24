import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as cartApi from '../api/cart'
import { extractErrorMessage } from '../api/errors'
import { useCart } from '../context/CartContext'

export function CartPage() {
  const navigate = useNavigate()
  const { cart, isLoading, refreshCart } = useCart()
  const [error, setError] = useState<string | null>(null)

  async function handleQuantityChange(cartItemId: number, quantity: number) {
    if (quantity < 1) return
    try {
      await cartApi.updateItemQuantity(cartItemId, quantity)
      await refreshCart()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  async function handleRemove(cartItemId: number) {
    try {
      await cartApi.removeItem(cartItemId)
      await refreshCart()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  async function handleClear() {
    try {
      await cartApi.clearCart()
      await refreshCart()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  if (isLoading) {
    return <p className="page">불러오는 중...</p>
  }

  return (
    <div className="page">
      <h1>장바구니</h1>
      {error && <p className="form-error">{error}</p>}

      {!cart || cart.items.length === 0 ? (
        <p>장바구니가 비어있습니다. <Link to="/">상품 보러 가기</Link></p>
      ) : (
        <>
          <div className="table-scroll">
          <table className="table">
            <thead>
              <tr>
                <th>상품</th>
                <th>가격</th>
                <th>수량</th>
                <th>합계</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {cart.items.map((item) => (
                <tr key={item.id}>
                  <td>{item.productName}</td>
                  <td>{item.price.toLocaleString()}원</td>
                  <td>
                    <input
                      type="number"
                      min={1}
                      value={item.quantity}
                      onChange={(e) => handleQuantityChange(item.id, Number(e.target.value))}
                      className="table__qty-input"
                    />
                  </td>
                  <td>{item.lineTotal.toLocaleString()}원</td>
                  <td>
                    <button type="button" onClick={() => handleRemove(item.id)}>
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>

          <div className="cart-summary">
            <span>총 합계: {cart.totalAmount.toLocaleString()}원</span>
            <div className="actions">
              <button type="button" onClick={handleClear}>
                장바구니 비우기
              </button>
              <button type="button" onClick={() => navigate('/checkout')}>
                주문하기
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
