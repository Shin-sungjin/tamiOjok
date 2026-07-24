import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as adminApi from '../../api/admin'
import { extractErrorMessage } from '../../api/errors'
import type { ProductResponse, ProductStatus } from '../../api/types'

const STATUS_LABEL: Record<ProductStatus, string> = {
  ON_SALE: '판매중',
  OUT_OF_STOCK: '품절',
  HIDDEN: '숨김',
}

const STATUS_BADGE_CLASS: Record<ProductStatus, string> = {
  ON_SALE: 'badge--success',
  OUT_OF_STOCK: 'badge--danger',
  HIDDEN: 'badge--muted',
}

export function AdminProductListPage() {
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [restockDrafts, setRestockDrafts] = useState<Record<number, string>>({})

  function load() {
    setIsLoading(true)
    adminApi
      .getAdminProducts(undefined, page)
      .then((result) => {
        setProducts(result.content)
        setTotalPages(result.totalPages)
        setError(null)
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }

  useEffect(load, [page])

  async function handleStatusChange(productId: number, status: ProductStatus) {
    setError(null)
    try {
      await adminApi.changeProductStatus(productId, status)
      load()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  async function handleRestock(productId: number) {
    const quantity = Number(restockDrafts[productId])
    if (!quantity || quantity <= 0) {
      return
    }
    setError(null)
    try {
      await adminApi.restockProduct(productId, quantity)
      setRestockDrafts((prev) => ({ ...prev, [productId]: '' }))
      load()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>상품 관리</h1>
        <Link to="/admin/products/new">
          <button type="button">새 상품 등록</button>
        </Link>
      </div>

      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}

      <div className="table-scroll">
      <table className="table">
        <thead>
          <tr>
            <th></th>
            <th>이름</th>
            <th>가격</th>
            <th>재고</th>
            <th>상태</th>
            <th>입고</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id}>
              <td>
                <div className="admin-table-thumb">
                  {product.imageUrls[0] ? (
                    <img src={product.imageUrls[0]} alt={product.name} />
                  ) : (
                    <span>-</span>
                  )}
                </div>
              </td>
              <td>
                <Link to={`/admin/products/${product.id}/edit`}>{product.name}</Link>
              </td>
              <td>{product.price.toLocaleString()}원</td>
              <td>{product.availableStock}개</td>
              <td>
                <span className={`badge ${STATUS_BADGE_CLASS[product.status]}`}>
                  {STATUS_LABEL[product.status]}
                </span>
              </td>
              <td>
                <div className="admin-inline-form">
                  <input
                    className="table__qty-input"
                    type="number"
                    min={1}
                    placeholder="수량"
                    value={restockDrafts[product.id] ?? ''}
                    onChange={(e) =>
                      setRestockDrafts((prev) => ({ ...prev, [product.id]: e.target.value }))
                    }
                  />
                  <button type="button" onClick={() => handleRestock(product.id)}>
                    입고
                  </button>
                </div>
              </td>
              <td>
                {product.status !== 'HIDDEN' ? (
                  <button type="button" onClick={() => handleStatusChange(product.id, 'HIDDEN')}>
                    숨김 처리
                  </button>
                ) : (
                  <button type="button" onClick={() => handleStatusChange(product.id, 'ON_SALE')}>
                    판매 재개
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            이전
          </button>
          <span>
            {page + 1} / {totalPages}
          </span>
          <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            다음
          </button>
        </div>
      )}
    </div>
  )
}
