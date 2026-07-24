import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as adminApi from '../../api/admin'
import { extractErrorMessage } from '../../api/errors'
import { BarChart } from '../../components/charts/BarChart'
import { DonutChart } from '../../components/charts/DonutChart'
import type { DashboardSummaryResponse } from '../../api/types'

export function AdminDashboardPage() {
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    adminApi
      .getDashboardSummary()
      .then(setSummary)
      .catch((err) => setError(extractErrorMessage(err)))
  }, [])

  if (error) {
    return <p className="form-error">{error}</p>
  }

  if (!summary) {
    return <p>불러오는 중...</p>
  }

  return (
    <div>
      <h1>대시보드</h1>

      <div className="stat-grid">
        <div className="stat-tile stat-tile--primary">
          <p className="stat-tile__label">누적 매출액</p>
          <p className="stat-tile__value">{summary.totalRevenue.toLocaleString()}원</p>
        </div>
        <div className="stat-tile">
          <p className="stat-tile__label">결제 완료 건수</p>
          <p className="stat-tile__value">{summary.paidOrderCount.toLocaleString()}건</p>
        </div>
        <div className="stat-tile">
          <p className="stat-tile__label">오늘 방문자수</p>
          <p className="stat-tile__value">{summary.todayVisitCount.toLocaleString()}명</p>
        </div>
        <div className="stat-tile">
          <p className="stat-tile__label">누적 방문자수</p>
          <p className="stat-tile__value">{summary.totalVisitCount.toLocaleString()}명</p>
        </div>
      </div>

      <div className="chart-grid">
        <div className="chart-panel">
          <h2 className="admin-section-title admin-section-title--tight">주문 현황</h2>
          <BarChart
            data={[
              { label: '결제대기', value: summary.pendingPaymentOrderCount },
              { label: '상품준비중', value: summary.preparingOrderCount },
              { label: '취소', value: summary.cancelledOrderCount },
            ]}
          />
        </div>

        <div className="chart-panel">
          <div className="page-header">
            <h2 className="admin-section-title admin-section-title--tight">배송 현황</h2>
            <Link to="/admin/orders?status=PREPARING">배송 등록 필요 →</Link>
          </div>
          <DonutChart
            data={[
              { label: '배송 등록 필요', value: summary.pendingShipmentCount },
              { label: '배송중', value: summary.inTransitCount },
              { label: '배송완료', value: summary.deliveredCount },
              { label: '반품요청', value: summary.returnRequestedCount },
            ]}
          />
        </div>
      </div>

      <h2 className="admin-section-title">고객 문의</h2>
      <div className="stat-grid">
        <Link to="/admin/inquiries" className="stat-tile stat-tile--link">
          <p className="stat-tile__label">답변 대기중인 문의</p>
          <p className="stat-tile__value">{summary.waitingInquiryCount.toLocaleString()}건</p>
        </Link>
      </div>
    </div>
  )
}
