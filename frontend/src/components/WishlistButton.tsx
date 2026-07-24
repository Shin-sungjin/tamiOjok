import { useWishlist } from '../hooks/useWishlist'

interface WishlistButtonProps {
  productId: number
}

export function WishlistButton({ productId }: WishlistButtonProps) {
  const { isWished, toggle } = useWishlist()
  const wished = isWished(productId)

  return (
    <button
      type="button"
      className={'wishlist-btn' + (wished ? ' wishlist-btn--active' : '')}
      aria-label={wished ? '찜 해제' : '찜하기'}
      aria-pressed={wished}
      onClick={(e) => {
        e.preventDefault()
        e.stopPropagation()
        toggle(productId)
      }}
    >
      {wished ? '♥' : '♡'}
    </button>
  )
}
