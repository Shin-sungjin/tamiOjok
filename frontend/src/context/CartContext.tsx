import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import * as cartApi from '../api/cart'
import { useAuth } from './AuthContext'
import type { CartResponse } from '../api/types'

interface CartContextValue {
  cart: CartResponse | null
  itemCount: number
  isLoading: boolean
  refreshCart: () => Promise<void>
}

const CartContext = createContext<CartContextValue | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const refreshCart = useCallback(async () => {
    if (!user) {
      setCart(null)
      setIsLoading(false)
      return
    }
    setIsLoading(true)
    try {
      setCart(await cartApi.getMyCart())
    } catch {
      setCart(null)
    } finally {
      setIsLoading(false)
    }
  }, [user])

  useEffect(() => {
    refreshCart()
  }, [refreshCart])

  // 헤더 뱃지는 "총 담긴 수량"이 아니라 "담긴 상품 종류 수"를 보여준다
  // (예: A 10개 + B 3개 → 13이 아니라 2). 한 상품을 대량으로 담아도 뱃지가
  // 과도하게 커지지 않고, 장바구니에 몇 종류를 담았는지 한눈에 보이는 게
  // 더 자연스럽다는 판단.
  const itemCount = cart?.items.length ?? 0

  return (
    <CartContext.Provider value={{ cart, itemCount, isLoading, refreshCart }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart(): CartContextValue {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error('useCart는 CartProvider 내부에서만 사용할 수 있습니다.')
  }
  return context
}
