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

export function useWishlist() {
  const [ids, setIds] = useState<number[]>(() => readWishlist())

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids))
  }, [ids])

  const isWished = useCallback((productId: number) => ids.includes(productId), [ids])

  const toggle = useCallback((productId: number) => {
    setIds((prev) => (prev.includes(productId) ? prev.filter((id) => id !== productId) : [...prev, productId]))
  }, [])

  return { isWished, toggle }
}
