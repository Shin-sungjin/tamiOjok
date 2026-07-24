import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as deliveriesApi from '../api/deliveries'
import * as ordersApi from '../api/orders'
import * as paymentsApi from '../api/payments'
import { extractErrorMessage } from '../api/errors'
import { DeliveryTimeline } from '../components/DeliveryTimeline'
import type { DeliveryResponse, OrderResponse, PaymentResponse } from '../api/types'

const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: '결제 대기',
  PAYMENT_COMPLETED: '결제 완료',
  PREPARING: '배송 준비중',
  CANCELLED: '취소됨',
}

const DELIVERY_STATUS_LABEL: Record<string, string> = {
  READY: '배송 준비중',
  SHIPPED: '발송됨',
  IN_TRANSIT: '배송중',
  DELIVERED: '배송 완료',
  RETURN_REQUESTED: '반품 요청됨',
}

const PAYMENT_STATUS_LABEL: Record<string, string> = {
  READY: '결제 대기',
  PAID: '결제 완료',
  FAILED: '결제 실패',
  CANCELLED: '결제 취소',
}

export function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [payment, setPayment] = useState<PaymentResponse | null>(null)
  const [delivery, setDelivery] = useState<DeliveryResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isProcessing, setIsProcessing] = useState(false)

  function loadOrder() {
    if (!orderId) return
    const id = Number(orderId)
    ordersApi
      .getMyOrder(id)
      .then(setOrder)
      .catch((err) => setError(extractErrorMessage(err)))
    paymentsApi.getPayment(id).then(setPayment).catch(() => setPayment(null))
    deliveriesApi.getDelivery(id).then(setDelivery).catch(() => setDelivery(null))
  }

  useEffect(loadOrder, [orderId])

  async function handlePay() {
    if (!order) return
    setActionError(null)
    setIsProcessing(true)
    try {
      await paymentsApi.confirmPayment({
        orderId: order.id,
        pgProvider: 'MOCK',
        pgTransactionId: `MOCK-${order.id}-${Date.now()}`,
        paidAmount: order.paymentAmount,
      })
      loadOrder()
    } catch (err) {
      setActionError(extractErrorMessage(err))
    } finally {
      setIsProcessing(false)
    }
  }

  async function handleCancel() {
    if (!order) return
    setActionError(null)
    setIsProcessing(true)
    try {
      await ordersApi.cancelOrder(order.id)
      loadOrder()
    } catch (err) {
      setActionError(extractErrorMessage(err))
    } finally {
      setIsProcessing(false)
    }
  }

  async function handleReturnRequest() {
    if (!order) return
    setActionError(null)
    setIsProcessing(true)
    try {
      await deliveriesApi.requestReturn(order.id)
      loadOrder()
    } catch (err) {
      setActionError(extractErrorMessage(err))
    } finally {
      setIsProcessing(false)
    }
  }

  if (error) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
        <Link to="/orders">주문 목록으로</Link>
      </div>
    )
  }

  if (!order) {
    return <p className="page">불러오는 중...</p>
  }

  const canCancel = order.status === 'PENDING_PAYMENT' || order.status === 'PREPARING'
  const canRequestReturn = delivery?.status === 'DELIVERED'

  return (
    <div className="page">
      <Link to="/orders">← 주문 목록</Link>
      <h1>주문 {order.orderNumber}</h1>
      <p>
        상태: <span className="badge">{ORDER_STATUS_LABEL[order.status] ?? order.status}</span>
      </p>

      <div className="table-scroll">
      <table className="table">
        <thead>
          <tr>
            <th>상품</th>
            <th>단가</th>
            <th>수량</th>
            <th>합계</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {order.items.map((item) => (
            <tr key={item.productId}>
              <td>{item.productName}</td>
              <td>{item.orderPrice.toLocaleString()}원</td>
              <td>{item.quantity}</td>
              <td>{item.lineTotal.toLocaleString()}원</td>
              <td>
                {delivery?.status === 'DELIVERED' && (
                  <Link to={`/reviews/new?orderId=${order.id}&productId=${item.productId}`}>
                    리뷰 작성
                  </Link>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>

      <div className="order-amounts">
        <p>상품 합계: {order.totalAmount.toLocaleString()}원</p>
        <p>할인 금액: -{order.discountAmount.toLocaleString()}원</p>
        <p className="order-amounts__total">결제 금액: {order.paymentAmount.toLocaleString()}원</p>
      </div>

      {payment && (
        <div className="detail-section">
          <h3>결제 정보</h3>
          <dl className="detail-list">
            <div>
              <dt>결제 상태</dt>
              <dd>
                <span className="badge">{PAYMENT_STATUS_LABEL[payment.status] ?? payment.status}</span>
              </dd>
            </div>
            <div>
              <dt>결제 수단</dt>
              <dd>{payment.pgProvider}</dd>
            </div>
            <div>
              <dt>거래 ID</dt>
              <dd>{payment.pgTransactionId}</dd>
            </div>
            <div>
              <dt>요청 금액</dt>
              <dd>{payment.requestedAmount.toLocaleString()}원</dd>
            </div>
            <div>
              <dt>실 결제 금액</dt>
              <dd>{payment.paidAmount != null ? `${payment.paidAmount.toLocaleString()}원` : '-'}</dd>
            </div>
            <div>
              <dt>결제 일시</dt>
              <dd>{payment.paidAt ? new Date(payment.paidAt).toLocaleString() : '-'}</dd>
            </div>
          </dl>
        </div>
      )}

      {delivery && (
        <div className="detail-section">
          <h3>배송 정보</h3>
          <DeliveryTimeline status={delivery.status} />
          <dl className="detail-list">
            <div>
              <dt>배송 상태</dt>
              <dd>
                <span className="badge">{DELIVERY_STATUS_LABEL[delivery.status] ?? delivery.status}</span>
              </dd>
            </div>
            <div>
              <dt>택배사</dt>
              <dd>{delivery.courierCode || '-'}</dd>
            </div>
            <div>
              <dt>송장번호</dt>
              <dd>{delivery.trackingNumber || '-'}</dd>
            </div>
            <div>
              <dt>발송일시</dt>
              <dd>{delivery.shippedAt ? new Date(delivery.shippedAt).toLocaleString() : '-'}</dd>
            </div>
            <div>
              <dt>배송완료일시</dt>
              <dd>{delivery.deliveredAt ? new Date(delivery.deliveredAt).toLocaleString() : '-'}</dd>
            </div>
          </dl>
        </div>
      )}

      {actionError && <p className="form-error">{actionError}</p>}

      <div className="actions">
        {order.status === 'PENDING_PAYMENT' && (
          <button type="button" onClick={handlePay} disabled={isProcessing}>
            결제하기 (Mock)
          </button>
        )}
        {canCancel && (
          <button type="button" onClick={handleCancel} disabled={isProcessing}>
            주문 취소
          </button>
        )}
        {canRequestReturn && (
          <button type="button" onClick={handleReturnRequest} disabled={isProcessing}>
            반품 요청
          </button>
        )}
        <button type="button" onClick={() => navigate(`/inquiries/new?orderId=${order.id}`)}>
          이 주문 문의하기
        </button>
      </div>
    </div>
  )
}
