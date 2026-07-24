import { NavLink, Outlet } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/products', label: '상품 관리', end: false },
  { to: '/admin/orders', label: '주문/배송 관리', end: false },
  { to: '/admin/inquiries', label: '문의 관리', end: false },
]

export function AdminLayout() {
  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <p className="admin-sidebar__title">관리자</p>
        <nav className="admin-sidebar__nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => 'admin-sidebar__link' + (isActive ? ' admin-sidebar__link--active' : '')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  )
}
