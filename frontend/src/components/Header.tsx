import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useWishlist } from '../hooks/useWishlist'

export function Header() {
  const { user, logout } = useAuth()
  const { itemCount } = useCart()
  const { ids: wishlistIds } = useWishlist()
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  function handleSearch(e: FormEvent) {
    e.preventDefault()
    const trimmed = keyword.trim()
    navigate(trimmed ? `/?keyword=${encodeURIComponent(trimmed)}` : '/')
  }

  return (
    <header className="header">
      <Link to="/" className="header__logo">
        <span aria-hidden="true">🐷</span> tamiOjok
      </Link>

      <form className="header__search" onSubmit={handleSearch} role="search">
        <input
          type="search"
          placeholder="상품 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          aria-label="상품 검색"
        />
        <button type="submit" aria-label="검색">
          🔍
        </button>
      </form>

      <nav className="header__nav">
        <Link to="/wishlist" className="header__icon-link" aria-label="찜한 상품">
          ♡
          {wishlistIds.length > 0 && <span className="header__cart-badge">{wishlistIds.length}</span>}
        </Link>
        {user ? (
          <>
            <Link to="/coupons">쿠폰</Link>
            <Link to="/cart" className="header__icon-link">
              🛒 장바구니
              {itemCount > 0 && <span className="header__cart-badge">{itemCount}</span>}
            </Link>
            <Link to="/orders">주문내역</Link>
            {user.role === 'ADMIN' && <Link to="/admin">관리자</Link>}
            <Link to="/mypage">{user.name}님</Link>
            <button type="button" onClick={handleLogout}>
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login">로그인</Link>
            <Link to="/signup">회원가입</Link>
          </>
        )}
      </nav>
    </header>
  )
}
