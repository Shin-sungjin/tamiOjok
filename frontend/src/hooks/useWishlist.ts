import { useCallback, useEffect, useState } from 'react'

const STORAGE_KEY = 'tamiojok_wishlist'

function readWishlist(): number[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as number[]) : []
  } catch {
    return []
  }
}

// 여러 컴포넌트(헤더, 상품카드, 상품상세)가 동시에 이 훅을 쓰므로, 한 곳에서
// 찜을 토글하면 다른 곳도 즉시 반영되도록 모듈 스코프의 공유 상태 + 구독자
// 목록으로 관리합니다 (계정 동기화는 안 되는 브라우저 로컬 전용 기능).
let wishlistIds: number[] = readWishlist()
const listeners = new Set<() => void>()

function setWishlistIds(next: number[]) {
  wishlistIds = next
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  listeners.forEach((listener) => listener())
}

export function useWishlist() {
  const [ids, setIds] = useState(wishlistIds)

  useEffect(() => {
    const listener = () => setIds(wishlistIds)
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  }, [])

  const isWished = useCallback((productId: number) => ids.includes(productId), [ids])

  const toggle = useCallback((productId: number) => {
    setWishlistIds(
      wishlistIds.includes(productId) ? wishlistIds.filter((id) => id !== productId) : [...wishlistIds, productId],
    )
  }, [])

  return { ids, isWished, toggle }
}
