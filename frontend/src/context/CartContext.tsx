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

  const itemCount = cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0

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
