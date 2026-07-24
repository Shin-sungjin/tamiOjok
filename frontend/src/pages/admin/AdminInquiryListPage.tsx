import { useEffect, useState } from 'react'
import * as adminApi from '../../api/admin'
import { extractErrorMessage } from '../../api/errors'
import type { InquiryResponse, InquiryStatus } from '../../api/types'

const TABS: { label: string; value: InquiryStatus | undefined }[] = [
  { label: '전체', value: undefined },
  { label: '답변대기', value: 'WAITING' },
  { label: '답변완료', value: 'ANSWERED' },
]

export function AdminInquiryListPage() {
  const [statusFilter, setStatusFilter] = useState<InquiryStatus | undefined>('WAITING')
  const [inquiries, setInquiries] = useState<InquiryResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [answerDrafts, setAnswerDrafts] = useState<Record<number, string>>({})
  const [submittingId, setSubmittingId] = useState<number | null>(null)

  function load() {
    setIsLoading(true)
    adminApi
      .getAdminInquiries(statusFilter)
      .then((result) => {
        setInquiries(result.content)
        setError(null)
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }

  useEffect(load, [statusFilter])

  async function handleAnswer(inquiryId: number) {
    const answer = (answerDrafts[inquiryId] ?? '').trim()
    if (!answer) {
      return
    }
    setSubmittingId(inquiryId)
    setError(null)
    try {
      await adminApi.answerInquiry(inquiryId, answer)
      load()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setSubmittingId(null)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>문의 관리</h1>
      </div>

      <div className="admin-tabs">
        {TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            className={'admin-tab' + (statusFilter === tab.value ? ' admin-tab--active' : '')}
            onClick={() => setStatusFilter(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && inquiries.length === 0 && <p>해당하는 문의가 없습니다.</p>}

      <ul className="list">
        {inquiries.map((inquiry) => (
          <li key={inquiry.id} className="card">
            <h3>{inquiry.title}</h3>
            <p className="card__meta">
              {inquiry.category} · <span className="badge">{inquiry.status === 'ANSWERED' ? '답변완료' : '대기중'}</span>{' '}
              · {new Date(inquiry.createdAt).toLocaleString()}
              {inquiry.orderId && ` · 주문번호 관련 #${inquiry.orderId}`}
            </p>
            <p>{inquiry.content}</p>

            {inquiry.answer ? (
              <div className="admin-answer-box">
                <p className="admin-answer-box__label">답변</p>
                <p>{inquiry.answer}</p>
                {inquiry.answeredAt && (
                  <p className="card__meta">{new Date(inquiry.answeredAt).toLocaleString()}</p>
                )}
              </div>
            ) : (
              <div className="admin-answer-form">
                <textarea
                  rows={3}
                  placeholder="답변을 입력하세요"
                  value={answerDrafts[inquiry.id] ?? ''}
                  onChange={(e) =>
                    setAnswerDrafts((prev) => ({ ...prev, [inquiry.id]: e.target.value }))
                  }
                />
                <button
                  type="button"
                  disabled={submittingId === inquiry.id || !(answerDrafts[inquiry.id] ?? '').trim()}
                  onClick={() => handleAnswer(inquiry.id)}
                >
                  {submittingId === inquiry.id ? '등록 중...' : '답변 등록'}
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
