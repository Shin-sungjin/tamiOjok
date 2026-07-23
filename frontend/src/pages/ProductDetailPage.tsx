import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getProduct } from '../api/products'
import { addItem } from '../api/cart'
import { getProductReviews } from '../api/reviews'
import { extractErrorMessage } from '../api/errors'
import { useAuth } from '../context/AuthContext'
import type { ProductResponse, ReviewResponse } from '../api/types'

export function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [reviews, setReviews] = useState<ReviewResponse[]>([])
  const [quantity, setQuantity] = useState(1)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!productId) return
    getProduct(Number(productId))
      .then(setProduct)
      .catch((err) => setError(extractErrorMessage(err)))
    getProductReviews(Number(productId))
      .then((result) => setReviews(result.content))
      .catch(() => setReviews([]))
  }, [productId])

  async function handleAddToCart() {
    if (!product) return
    if (!user) {
      navigate('/login')
      return
    }
    setError(null)
    setMessage(null)
    try {
      await addItem({ productId: product.id, quantity })
      setMessage('장바구니에 담았습니다.')
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  if (error && !product) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
        <Link to="/">목록으로 돌아가기</Link>
      </div>
    )
  }

  if (!product) {
    return <p className="page">불러오는 중...</p>
  }

  return (
    <div className="page">
      <Link to="/">← 목록으로</Link>
      <h1>{product.name}</h1>
      <p className="product-detail__price">{product.price.toLocaleString()}원</p>
      <p>{product.description ?? '상품 설명이 없습니다.'}</p>
      <p>재고: {product.availableStock}개</p>
      <p>판매 상태: {product.status}</p>

      <div className="actions">
        <input
          type="number"
          min={1}
          max={product.availableStock || undefined}
          value={quantity}
          onChange={(e) => setQuantity(Number(e.target.value))}
          className="table__qty-input"
        />
        <button type="button" onClick={handleAddToCart} disabled={product.availableStock <= 0}>
          장바구니 담기
        </button>
      </div>
      {message && <p>{message}</p>}
      {error && <p className="form-error">{error}</p>}

      <h2>리뷰</h2>
      {reviews.length === 0 ? (
        <p>아직 등록된 리뷰가 없습니다.</p>
      ) : (
        <ul className="list">
          {reviews.map((review) => (
            <li key={review.id} className="card">
              <p>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</p>
              <p>{review.content}</p>
              <p className="card__meta">{new Date(review.createdAt).toLocaleString()}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
