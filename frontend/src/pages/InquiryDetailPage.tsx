import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import * as inquiriesApi from '../api/inquiries'
import { extractErrorMessage } from '../api/errors'
import type { InquiryResponse } from '../api/types'

export function InquiryDetailPage() {
  const { inquiryId } = useParams<{ inquiryId: string }>()
  const [inquiry, setInquiry] = useState<InquiryResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!inquiryId) return
    inquiriesApi
      .getMyInquiry(Number(inquiryId))
      .then(setInquiry)
      .catch((err) => setError(extractErrorMessage(err)))
  }, [inquiryId])

  if (error) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
        <Link to="/inquiries">목록으로</Link>
      </div>
    )
  }

  if (!inquiry) {
    return <p className="page">불러오는 중...</p>
  }

  return (
    <div className="page">
      <Link to="/inquiries">← 문의 목록</Link>
      <h1>{inquiry.title}</h1>
      <p className="card__meta">
        {inquiry.category} · <span className="badge">{inquiry.status === 'ANSWERED' ? '답변완료' : '대기중'}</span>{' '}
        · {new Date(inquiry.createdAt).toLocaleString()}
      </p>
      <p>{inquiry.content}</p>

      {inquiry.answer ? (
        <div className="card">
          <h3>답변</h3>
          <p>{inquiry.answer}</p>
          {inquiry.answeredAt && <p className="card__meta">{new Date(inquiry.answeredAt).toLocaleString()}</p>}
        </div>
      ) : (
        <p>아직 답변이 등록되지 않았습니다.</p>
      )}
    </div>
  )
}
