import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import * as adminApi from '../../api/admin'
import { extractErrorMessage } from '../../api/errors'
import type { AdminOrderResponse, OrderStatus } from '../../api/types'

const TABS: { label: string; value: OrderStatus | undefined }[] = [
  { label: '전체', value: undefined },
  { label: '결제완료', value: 'PAYMENT_COMPLETED' },
  { label: '상품준비중', value: 'PREPARING' },
  { label: '결제대기', value: 'PENDING_PAYMENT' },
  { label: '취소', value: 'CANCELLED' },
]

const DELIVERY_STATUS_LABEL: Record<string, string> = {
  READY: '배송준비',
  SHIPPED: '발송됨',
  IN_TRANSIT: '배송중',
  DELIVERED: '배송완료',
  RETURN_REQUESTED: '반품요청',
}

export function AdminOrderListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const statusFilter = (searchParams.get('status') as OrderStatus | null) ?? undefined
  const [orders, setOrders] = useState<AdminOrderResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [busyOrderId, setBusyOrderId] = useState<number | null>(null)
  const [deliveryDrafts, setDeliveryDrafts] = useState<Record<number, { courierCode: string; trackingNumber: string }>>({})

  function load() {
    setIsLoading(true)
    adminApi
      .getAdminOrders(statusFilter)
      .then((result) => {
        setOrders(result.content)
        setError(null)
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }

  useEffect(load, [statusFilter])

  async function handlePrepare(orderId: number) {
    setBusyOrderId(orderId)
    setError(null)
    try {
      await adminApi.startPreparingOrder(orderId)
      load()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setBusyOrderId(null)
    }
  }

  async function handleCreateDelivery(orderId: number) {
    const draft = deliveryDrafts[orderId]
    if (!draft?.courierCode.trim() || !draft?.trackingNumber.trim()) {
      return
    }
    setBusyOrderId(orderId)
    setError(null)
    try {
      await adminApi.createDelivery(orderId, draft)
      load()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setBusyOrderId(null)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>주문/배송 관리</h1>
      </div>

      <div className="admin-tabs">
        {TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            className={'admin-tab' + (statusFilter === tab.value ? ' admin-tab--active' : '')}
            onClick={() => setSearchParams(tab.value ? { status: tab.value } : {})}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && orders.length === 0 && <p>해당하는 주문이 없습니다.</p>}

      <ul className="list">
        {orders.map((order) => (
          <li key={order.id} className="card">
            <div className="page-header">
              <h3>{order.orderNumber}</h3>
              <span className="badge">{order.paymentAmount.toLocaleString()}원</span>
            </div>
            <p className="card__meta">
              {order.buyerName} ({order.buyerEmail}) · {new Date(order.createdAt).toLocaleString()}
            </p>
            <p className="card__meta">
              주문상태: <span className="badge">{order.status}</span>
              {order.deliveryStatus && (
                <>
                  {' '}
                  · 배송상태: <span className="badge">{DELIVERY_STATUS_LABEL[order.deliveryStatus]}</span>
                  {order.trackingNumber && ` (${order.trackingNumber})`}
                </>
              )}
            </p>
            <ul>
              {order.items.map((item) => (
                <li key={item.productId}>
                  {item.productName} × {item.quantity} = {item.lineTotal.toLocaleString()}원
                </li>
              ))}
            </ul>

            {order.status === 'PAYMENT_COMPLETED' && (
              <div className="actions">
                <button type="button" disabled={busyOrderId === order.id} onClick={() => handlePrepare(order.id)}>
                  상품 준비 시작
                </button>
              </div>
            )}

            {order.status === 'PREPARING' && !order.deliveryStatus && (
              <div className="admin-inline-form">
                <input
                  placeholder="택배사 코드 (예: CJGLS)"
                  value={deliveryDrafts[order.id]?.courierCode ?? ''}
                  onChange={(e) =>
                    setDeliveryDrafts((prev) => ({
                      ...prev,
                      [order.id]: { courierCode: e.target.value, trackingNumber: prev[order.id]?.trackingNumber ?? '' },
                    }))
                  }
                />
                <input
                  placeholder="운송장 번호"
                  value={deliveryDrafts[order.id]?.trackingNumber ?? ''}
                  onChange={(e) =>
                    setDeliveryDrafts((prev) => ({
                      ...prev,
                      [order.id]: { courierCode: prev[order.id]?.courierCode ?? '', trackingNumber: e.target.value },
                    }))
                  }
                />
                <button
                  type="button"
                  disabled={busyOrderId === order.id}
                  onClick={() => handleCreateDelivery(order.id)}
                >
                  배송 등록
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
