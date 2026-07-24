import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProduct } from '../api/products'
import { useWishlist } from '../hooks/useWishlist'
import { useMyCoupons } from '../hooks/useMyCoupons'
import { RatingStars } from '../components/RatingStars'
import { WishlistButton } from '../components/WishlistButton'
import { PriceDisplay } from '../components/PriceDisplay'
import type { ProductResponse } from '../api/types'

export function WishlistPage() {
  const { ids } = useWishlist()
  const coupons = useMyCoupons()
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (ids.length === 0) {
      setProducts([])
      setIsLoading(false)
      return
    }
    setIsLoading(true)
    Promise.all(ids.map((id) => getProduct(id).catch(() => null))).then((results) => {
      setProducts(results.filter((product): product is ProductResponse => product !== null))
      setIsLoading(false)
    })
  }, [ids])

  return (
    <div className="page">
      <h1>찜한 상품</h1>
      {isLoading && <p>불러오는 중...</p>}
      {!isLoading && products.length === 0 && (
        <p>
          찜한 상품이 없습니다. <Link to="/">상품 보러 가기</Link>
        </p>
      )}

      <ul className="product-grid">
        {products.map((product) => (
          <li key={product.id} className="product-card">
            <WishlistButton productId={product.id} />
            <Link to={`/products/${product.id}`}>
              <div className="product-card__image">
                {product.imageUrls[0] ? (
                  <img src={product.imageUrls[0]} alt={product.name} loading="lazy" />
                ) : (
                  <span className="product-card__image-placeholder">이미지 준비중</span>
                )}
              </div>
              <h2>{product.name}</h2>
              <RatingStars averageRating={product.averageRating} reviewCount={product.reviewCount} />
              <PriceDisplay price={product.price} coupons={coupons} size="sm" />
              {product.availableStock <= 0 && <p className="product-card__stock">품절</p>}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
