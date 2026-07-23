import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import * as inquiriesApi from '../api/inquiries'
import { extractErrorMessage } from '../api/errors'

export function InquiryCreatePage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const orderIdParam = searchParams.get('orderId')
  const [category, setCategory] = useState('배송')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await inquiriesApi.createInquiry({
        category,
        title,
        content,
        orderId: orderIdParam ? Number(orderIdParam) : undefined,
      })
      navigate('/inquiries')
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="form-page">
      <h1>문의 작성</h1>
      <form onSubmit={handleSubmit}>
        <label>
          카테고리
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="배송">배송</option>
            <option value="상품">상품</option>
            <option value="결제/환불">결제/환불</option>
            <option value="기타">기타</option>
          </select>
        </label>
        <label>
          제목
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} required />
        </label>
        <label>
          내용
          <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={5} required />
        </label>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '등록 중...' : '문의 등록'}
        </button>
      </form>
    </div>
  )
}
