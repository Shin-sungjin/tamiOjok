import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getProduct } from '../api/products'
import { addItem } from '../api/cart'
import { createOrder } from '../api/orders'
import { getProductReviews } from '../api/reviews'
import { extractErrorMessage } from '../api/errors'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useMyCoupons } from '../hooks/useMyCoupons'
import { PriceDisplay } from '../components/PriceDisplay'
import { RatingStars } from '../components/RatingStars'
import { WishlistButton } from '../components/WishlistButton'
import type { ProductResponse, ReviewResponse } from '../api/types'

const STATUS_LABEL: Record<string, string> = {
  ON_SALE: '판매중',
  OUT_OF_STOCK: '품절',
  HIDDEN: '판매중지',
}

const STATUS_BADGE_CLASS: Record<string, string> = {
  ON_SALE: 'badge--success',
  OUT_OF_STOCK: 'badge--danger',
  HIDDEN: 'badge--muted',
}

export function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>()
  const { user } = useAuth()
  const { refreshCart } = useCart()
  const navigate = useNavigate()
  const coupons = useMyCoupons()

  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [reviews, setReviews] = useState<ReviewResponse[]>([])
  const [activeImage, setActiveImage] = useState(0)
  const [tab, setTab] = useState<'detail' | 'reviews'>('detail')
  const [quantity, setQuantity] = useState(1)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!productId) return
    getProduct(Number(productId))
      .then((result) => {
        setProduct(result)
        setActiveImage(0)
      })
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
      await refreshCart()
      setMessage('장바구니에 담았습니다.')
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  async function handleBuyNow() {
    if (!product) return
    if (!user) {
      navigate('/login')
      return
    }
    setError(null)
    setMessage(null)
    try {
      const order = await createOrder({ items: [{ productId: product.id, quantity }] })
      navigate(`/orders/${order.id}`)
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

  const images = product.imageUrls.length > 0 ? product.imageUrls : []

  return (
    <div className="page">
      <Link to="/">← 목록으로</Link>

      <div className="product-detail">
        <div className="product-detail__gallery">
          <WishlistButton productId={product.id} />
          <div className="product-detail__main-image">
            {images[activeImage] ? (
              <img src={images[activeImage]} alt={product.name} />
            ) : (
              <span className="product-card__image-placeholder">이미지 준비중</span>
            )}
          </div>
          {images.length > 1 && (
            <div className="product-detail__thumbs">
              {images.map((url, index) => (
                <button
                  key={url + index}
                  type="button"
                  className={
                    'product-detail__thumb' + (index === activeImage ? ' product-detail__thumb--active' : '')
                  }
                  onClick={() => setActiveImage(index)}
                >
                  <img src={url} alt={`${product.name} ${index + 1}`} />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="product-detail__info">
          <h1>{product.name}</h1>
          <RatingStars averageRating={product.averageRating} reviewCount={product.reviewCount} />
          <PriceDisplay price={product.price} coupons={coupons} size="md" />

          <p className="card__meta">
            <span
              className={`badge ${
                product.status !== 'HIDDEN' && product.availableStock <= 0
                  ? 'badge--danger'
                  : STATUS_BADGE_CLASS[product.status]
              }`}
            >
              {product.status !== 'HIDDEN' && product.availableStock <= 0
                ? '품절'
                : STATUS_LABEL[product.status]}
            </span>
          </p>

          <label className="product-detail__qty-label">
            수량
            <input
              type="number"
              min={1}
              max={product.availableStock || undefined}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
              className="table__qty-input"
            />
          </label>

          <div className="product-detail__cta">
            <button
              type="button"
              className="btn-primary"
              onClick={handleBuyNow}
              disabled={product.availableStock <= 0}
            >
              바로 구매
            </button>
            <button type="button" onClick={handleAddToCart} disabled={product.availableStock <= 0}>
              장바구니 담기
            </button>
          </div>
          {message && <p>{message}</p>}
          {error && <p className="form-error">{error}</p>}
        </div>
      </div>

      <div className="product-detail__tabs">
        <div className="admin-tabs">
          <button
            type="button"
            className={'admin-tab' + (tab === 'detail' ? ' admin-tab--active' : '')}
            onClick={() => setTab('detail')}
          >
            상세정보
          </button>
          <button
            type="button"
            className={'admin-tab' + (tab === 'reviews' ? ' admin-tab--active' : '')}
            onClick={() => setTab('reviews')}
          >
            리뷰 {reviews.length}
          </button>
        </div>

        {tab === 'detail' && (
          <p className="product-detail__description">{product.description ?? '상품 설명이 없습니다.'}</p>
        )}

        {tab === 'reviews' &&
          (reviews.length === 0 ? (
            <p>아직 등록된 리뷰가 없습니다.</p>
          ) : (
            <ul className="list">
              {reviews.map((review) => (
                <li key={review.id} className="card">
                  <p>
                    {'★'.repeat(review.rating)}
                    {'☆'.repeat(5 - review.rating)}
                  </p>
                  <p>{review.content}</p>
                  <p className="card__meta">{new Date(review.createdAt).toLocaleString()}</p>
                </li>
              ))}
            </ul>
          ))}
      </div>
    </div>
  )
}
