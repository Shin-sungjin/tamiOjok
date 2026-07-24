import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getProducts } from '../api/products'
import { extractErrorMessage } from '../api/errors'
import { useMyCoupons } from '../hooks/useMyCoupons'
import { PriceDisplay } from '../components/PriceDisplay'
import { RatingStars } from '../components/RatingStars'
import { WishlistButton } from '../components/WishlistButton'
import type { ProductResponse } from '../api/types'

export function ProductListPage() {
  const [searchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const coupons = useMyCoupons()

  useEffect(() => {
    setPage(0)
  }, [keyword])

  useEffect(() => {
    setIsLoading(true)
    getProducts(page, 12, keyword)
      .then((result) => {
        setProducts(result.content)
        setTotalPages(result.totalPages)
        setError(null)
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [page, keyword])

  return (
    <div className="page">
      <h1>{keyword ? `'${keyword}' 검색결과` : '상품 목록'}</h1>
      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && products.length === 0 && <p>검색 결과가 없습니다.</p>}

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

      {totalPages > 1 && (
        <div className="pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            이전
          </button>
          <span>
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </button>
        </div>
      )}
    </div>
  )
}
