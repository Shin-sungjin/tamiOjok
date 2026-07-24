interface RatingStarsProps {
  averageRating: number | null
  reviewCount: number
}

export function RatingStars({ averageRating, reviewCount }: RatingStarsProps) {
  if (!averageRating || reviewCount === 0) {
    return null
  }

  return (
    <p className="rating-stars">
      <span className="rating-stars__icon" aria-hidden="true">
        ★
      </span>
      <span className="rating-stars__value">{averageRating.toFixed(1)}</span>
      <span className="rating-stars__count">({reviewCount})</span>
    </p>
  )
}
