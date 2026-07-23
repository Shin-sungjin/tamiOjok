import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as inquiriesApi from '../api/inquiries'
import { extractErrorMessage } from '../api/errors'
import type { InquiryResponse } from '../api/types'

export function InquiryListPage() {
  const [inquiries, setInquiries] = useState<InquiryResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    inquiriesApi
      .getMyInquiries()
      .then((result) => setInquiries(result.content))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [])

  return (
    <div className="page">
      <div className="page-header">
        <h1>1:1 문의</h1>
        <Link to="/inquiries/new">
          <button type="button">새 문의 작성</button>
        </Link>
      </div>
      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && inquiries.length === 0 && <p>문의 내역이 없습니다.</p>}

      <ul className="list">
        {inquiries.map((inquiry) => (
          <li key={inquiry.id} className="card">
            <Link to={`/inquiries/${inquiry.id}`}>
              <h3>{inquiry.title}</h3>
              <p className="card__meta">
                {inquiry.category} · <span className="badge">{inquiry.status === 'ANSWERED' ? '답변완료' : '대기중'}</span>{' '}
                · {new Date(inquiry.createdAt).toLocaleString()}
              </p>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
