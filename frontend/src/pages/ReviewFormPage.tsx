import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import * as reviewsApi from '../api/reviews'
import { extractErrorMessage } from '../api/errors'

export function ReviewFormPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const orderId = Number(searchParams.get('orderId'))
  const productId = Number(searchParams.get('productId'))
  const [rating, setRating] = useState(5)
  const [content, setContent] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await reviewsApi.createReview({ orderId, productId, rating, content })
      navigate('/reviews')
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!orderId || !productId) {
    return (
      <div className="page">
        <p className="form-error">잘못된 접근입니다. 주문 상세에서 리뷰 작성을 눌러주세요.</p>
      </div>
    )
  }

  return (
    <div className="form-page">
      <h1>리뷰 작성</h1>
      <form onSubmit={handleSubmit}>
        <label>
          평점
          <select value={rating} onChange={(e) => setRating(Number(e.target.value))}>
            {[5, 4, 3, 2, 1].map((n) => (
              <option key={n} value={n}>
                {n}점
              </option>
            ))}
          </select>
        </label>
        <label>
          내용
          <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={5} required />
        </label>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '등록 중...' : '리뷰 등록'}
        </button>
      </form>
    </div>
  )
}
