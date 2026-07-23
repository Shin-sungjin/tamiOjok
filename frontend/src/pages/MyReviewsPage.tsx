import { useEffect, useState } from 'react'
import * as reviewsApi from '../api/reviews'
import { extractErrorMessage } from '../api/errors'
import type { ReviewResponse } from '../api/types'

export function MyReviewsPage() {
  const [reviews, setReviews] = useState<ReviewResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editRating, setEditRating] = useState(5)
  const [editContent, setEditContent] = useState('')

  function loadReviews() {
    setIsLoading(true)
    reviewsApi
      .getMyReviews()
      .then((result) => setReviews(result.content))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }

  useEffect(loadReviews, [])

  function startEdit(review: ReviewResponse) {
    setEditingId(review.id)
    setEditRating(review.rating)
    setEditContent(review.content)
  }

  async function handleSave(reviewId: number) {
    try {
      await reviewsApi.updateReview(reviewId, { rating: editRating, content: editContent })
      setEditingId(null)
      loadReviews()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  async function handleDelete(reviewId: number) {
    try {
      await reviewsApi.deleteReview(reviewId)
      loadReviews()
    } catch (err) {
      setError(extractErrorMessage(err))
    }
  }

  if (isLoading) {
    return <p className="page">불러오는 중...</p>
  }

  return (
    <div className="page">
      <h1>내 리뷰</h1>
      {error && <p className="form-error">{error}</p>}
      {reviews.length === 0 && <p>작성한 리뷰가 없습니다.</p>}

      <ul className="list">
        {reviews.map((review) => (
          <li key={review.id} className="card">
            <h3>{review.productName}</h3>
            {editingId === review.id ? (
              <>
                <select value={editRating} onChange={(e) => setEditRating(Number(e.target.value))}>
                  {[5, 4, 3, 2, 1].map((n) => (
                    <option key={n} value={n}>
                      {n}점
                    </option>
                  ))}
                </select>
                <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} rows={3} />
                <div className="actions">
                  <button type="button" onClick={() => handleSave(review.id)}>
                    저장
                  </button>
                  <button type="button" onClick={() => setEditingId(null)}>
                    취소
                  </button>
                </div>
              </>
            ) : (
              <>
                <p>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</p>
                <p>{review.content}</p>
                <p className="card__meta">{new Date(review.createdAt).toLocaleString()}</p>
                <div className="actions">
                  <button type="button" onClick={() => startEdit(review)}>
                    수정
                  </button>
                  <button type="button" onClick={() => handleDelete(review.id)}>
                    삭제
                  </button>
                </div>
              </>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
