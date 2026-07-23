import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function MyPage() {
  const { user } = useAuth()

  if (!user) {
    return null
  }

  return (
    <div className="page">
      <h1>마이페이지</h1>
      <div className="card">
        <p>이름: {user.name}</p>
        <p>이메일: {user.email}</p>
        <p>연락처: {user.phoneNumber ?? '미등록'}</p>
        <p>가입 경로: {user.provider}</p>
      </div>

      <ul className="list">
        <li className="card">
          <Link to="/orders">주문 내역</Link>
        </li>
        <li className="card">
          <Link to="/cart">장바구니</Link>
        </li>
        <li className="card">
          <Link to="/reviews">내 리뷰</Link>
        </li>
        <li className="card">
          <Link to="/inquiries">1:1 문의</Link>
        </li>
        <li className="card">
          <Link to="/coupons/my">내 쿠폰함</Link>
        </li>
      </ul>
    </div>
  )
}
