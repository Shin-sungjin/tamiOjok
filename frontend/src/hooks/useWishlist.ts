import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'

const STORAGE_PREFIX = 'tamiojok_wishlist'

function storageKeyFor(userId: number | null): string {
  // 비로그인 상태는 'guest' 버킷을 씀 — 계정과 무관하게 누구나 찜 기능을
  // 쓸 수 있게 한 기존 설계(로컬 전용 기능)는 유지하되, 로그인 사용자 간에는
  // 서로의 찜 목록이 보이거나 수정되지 않도록 계정별로 버킷을 분리한다.
  return userId != null ? `${STORAGE_PREFIX}:${userId}` : `${STORAGE_PREFIX}:guest`
}

function readWishlist(key: string): number[] {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as number[]) : []
  } catch {
    return []
  }
}

// 여러 컴포넌트(헤더, 상품카드, 상품상세)가 동시에 이 훅을 쓰므로, 한 곳에서
// 찜을 토글하면 다른 곳도 즉시 반영되도록 모듈 스코프의 공유 상태 + 구독자
// 목록으로 관리합니다 (계정 동기화는 안 되는 브라우저 로컬 전용 기능).
//
// 이전엔 저장 키가 고정이라, 로그인한 사용자가 찜한 뒤 로그아웃해도(또는 같은
// 브라우저에서 다른 계정으로 로그인해도) 이전 사용자의 찜 목록이 그대로
// 보이고 수정까지 가능한 문제가 있었음 — 로그인 사용자 id(없으면 guest)
// 기준으로 버킷을 분리해서 해결.
let currentKey = storageKeyFor(null)
let wishlistIds: number[] = readWishlist(currentKey)
const listeners = new Set<() => void>()

function setWishlistIds(next: number[]) {
  wishlistIds = next
  localStorage.setItem(currentKey, JSON.stringify(next))
  listeners.forEach((listener) => listener())
}

function switchBucket(nextKey: string) {
  if (nextKey === currentKey) {
    return
  }
  currentKey = nextKey
  wishlistIds = readWishlist(currentKey)
  listeners.forEach((listener) => listener())
}

export function useWishlist() {
  const { user } = useAuth()
  const [ids, setIds] = useState(wishlistIds)

  useEffect(() => {
    switchBucket(storageKeyFor(user?.id ?? null))
  }, [user])

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
