import type { DeliveryStatus, OrderStatus } from '../api/types'

const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '결제 대기',
  PAYMENT_COMPLETED: '결제 완료',
  PREPARING: '배송 준비중',
  CANCELLED: '취소됨',
}

const DELIVERY_STATUS_LABEL: Record<DeliveryStatus, string> = {
  READY: '배송 준비중',
  SHIPPED: '발송됨',
  IN_TRANSIT: '배송중',
  DELIVERED: '배송 완료',
  RETURN_REQUESTED: '반품 요청됨',
}

// order.status는 결제/이행 단계(PENDING_PAYMENT→PAYMENT_COMPLETED→PREPARING)만
// 나타내고 배송이 등록/진행/완료/반품요청 되어도 PREPARING에 머무름 — 배송
// 레코드가 있으면 더 구체적인 배송 상태를 우선 보여줘야, 주문목록과 주문상세가
// 서로 다른 상태를 보여주는 문제가 재발하지 않는다.
export function getOrderStatusLabel(orderStatus: OrderStatus, deliveryStatus: DeliveryStatus | null): string {
  if (orderStatus === 'CANCELLED') {
    return ORDER_STATUS_LABEL.CANCELLED
  }
  if (deliveryStatus) {
    return DELIVERY_STATUS_LABEL[deliveryStatus] ?? deliveryStatus
  }
  return ORDER_STATUS_LABEL[orderStatus] ?? orderStatus
}

export { DELIVERY_STATUS_LABEL, ORDER_STATUS_LABEL }
