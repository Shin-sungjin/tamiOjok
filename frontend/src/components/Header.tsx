import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  return (
    <header className="header">
      <Link to="/" className="header__logo">
        <span aria-hidden="true">🐷</span> tamiOjok
      </Link>
      <nav className="header__nav">
        {user ? (
          <>
            <Link to="/coupons">쿠폰</Link>
            <Link to="/cart">장바구니</Link>
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
