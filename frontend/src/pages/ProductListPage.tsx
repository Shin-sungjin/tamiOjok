import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProducts } from '../api/products'
import { extractErrorMessage } from '../api/errors'
import type { ProductResponse } from '../api/types'

export function ProductListPage() {
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    setIsLoading(true)
    getProducts(page)
      .then((result) => {
        setProducts(result.content)
        setTotalPages(result.totalPages)
        setError(null)
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [page])

  return (
    <div className="page">
      <h1>상품 목록</h1>
      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}

      <ul className="product-grid">
        {products.map((product) => (
          <li key={product.id} className="product-card">
            <Link to={`/products/${product.id}`}>
              <h2>{product.name}</h2>
              <p>{product.price.toLocaleString()}원</p>
              <p className="product-card__stock">재고 {product.availableStock}개</p>
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
