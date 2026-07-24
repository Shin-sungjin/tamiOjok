import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as ordersApi from '../api/orders'
import { extractErrorMessage } from '../api/errors'
import type { OrderResponse } from '../api/types'

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: '결제 대기',
  PAYMENT_COMPLETED: '결제 완료',
  PREPARING: '배송 준비중',
  CANCELLED: '취소됨',
}

export function OrderListPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    setIsLoading(true)
    ordersApi
      .getMyOrders(page)
      .then((result) => {
        setOrders(result.content)
        setTotalPages(result.totalPages)
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [page])

  return (
    <div className="page">
      <h1>주문 내역</h1>
      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}

      {!isLoading && orders.length === 0 && <p>주문 내역이 없습니다.</p>}

      {orders.length > 0 && (
        <div className="table-scroll">
        <table className="table">
          <thead>
            <tr>
              <th>주문번호</th>
              <th>상품</th>
              <th>상태</th>
              <th>결제금액</th>
              <th>주문일</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => {
              const [first, ...rest] = order.items
              const itemPreview = first
                ? rest.length > 0
                  ? `${first.productName} 외 ${rest.length}건`
                  : first.productName
                : '-'
              return (
                <tr key={order.id}>
                  <td>{order.orderNumber}</td>
                  <td>{itemPreview}</td>
                  <td>
                    <span className="badge">{STATUS_LABEL[order.status] ?? order.status}</span>
                  </td>
                  <td>{order.paymentAmount.toLocaleString()}원</td>
                  <td>{new Date(order.createdAt).toLocaleString()}</td>
                  <td>
                    <Link to={`/orders/${order.id}`}>상세보기</Link>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
        </div>
      )}

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
