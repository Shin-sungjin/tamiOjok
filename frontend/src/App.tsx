import { Suspense, lazy, useEffect } from 'react'
import { Route, Routes } from 'react-router-dom'
import { Header } from './components/Header'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { ProductListPage } from './pages/ProductListPage'
import { trackVisitOnce } from './api/track'

const AdminLayout = lazy(() => import('./components/AdminLayout').then((m) => ({ default: m.AdminLayout })))
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const SignupPage = lazy(() => import('./pages/SignupPage').then((m) => ({ default: m.SignupPage })))
const ProductDetailPage = lazy(() =>
  import('./pages/ProductDetailPage').then((m) => ({ default: m.ProductDetailPage })),
)
const WishlistPage = lazy(() => import('./pages/WishlistPage').then((m) => ({ default: m.WishlistPage })))
const CartPage = lazy(() => import('./pages/CartPage').then((m) => ({ default: m.CartPage })))
const CheckoutPage = lazy(() => import('./pages/CheckoutPage').then((m) => ({ default: m.CheckoutPage })))
const OrderListPage = lazy(() => import('./pages/OrderListPage').then((m) => ({ default: m.OrderListPage })))
const OrderDetailPage = lazy(() =>
  import('./pages/OrderDetailPage').then((m) => ({ default: m.OrderDetailPage })),
)
const ReviewFormPage = lazy(() => import('./pages/ReviewFormPage').then((m) => ({ default: m.ReviewFormPage })))
const MyReviewsPage = lazy(() => import('./pages/MyReviewsPage').then((m) => ({ default: m.MyReviewsPage })))
const InquiryListPage = lazy(() =>
  import('./pages/InquiryListPage').then((m) => ({ default: m.InquiryListPage })),
)
const InquiryCreatePage = lazy(() =>
  import('./pages/InquiryCreatePage').then((m) => ({ default: m.InquiryCreatePage })),
)
const InquiryDetailPage = lazy(() =>
  import('./pages/InquiryDetailPage').then((m) => ({ default: m.InquiryDetailPage })),
)
const CouponListPage = lazy(() =>
  import('./pages/CouponListPage').then((m) => ({ default: m.CouponListPage })),
)
const MyCouponsPage = lazy(() => import('./pages/MyCouponsPage').then((m) => ({ default: m.MyCouponsPage })))
const AdditionalInfoPage = lazy(() =>
  import('./pages/AdditionalInfoPage').then((m) => ({ default: m.AdditionalInfoPage })),
)
const MyPage = lazy(() => import('./pages/MyPage').then((m) => ({ default: m.MyPage })))
const AdminDashboardPage = lazy(() =>
  import('./pages/admin/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage })),
)
const AdminProductListPage = lazy(() =>
  import('./pages/admin/AdminProductListPage').then((m) => ({ default: m.AdminProductListPage })),
)
const AdminProductFormPage = lazy(() =>
  import('./pages/admin/AdminProductFormPage').then((m) => ({ default: m.AdminProductFormPage })),
)
const AdminOrderListPage = lazy(() =>
  import('./pages/admin/AdminOrderListPage').then((m) => ({ default: m.AdminOrderListPage })),
)
const AdminInquiryListPage = lazy(() =>
  import('./pages/admin/AdminInquiryListPage').then((m) => ({ default: m.AdminInquiryListPage })),
)

function App() {
  useEffect(() => {
    trackVisitOnce()
  }, [])

  return (
    <>
      <Header />
      <main>
        <Suspense fallback={<p className="page">불러오는 중...</p>}>
        <Routes>
          <Route path="/" element={<ProductListPage />} />
          <Route path="/products/:productId" element={<ProductDetailPage />} />
          <Route path="/wishlist" element={<WishlistPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route
            path="/additional-info"
            element={
              <ProtectedRoute>
                <AdditionalInfoPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/mypage"
            element={
              <ProtectedRoute>
                <MyPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/cart"
            element={
              <ProtectedRoute>
                <CartPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/checkout"
            element={
              <ProtectedRoute>
                <CheckoutPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <OrderListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders/:orderId"
            element={
              <ProtectedRoute>
                <OrderDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/reviews"
            element={
              <ProtectedRoute>
                <MyReviewsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/reviews/new"
            element={
              <ProtectedRoute>
                <ReviewFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inquiries"
            element={
              <ProtectedRoute>
                <InquiryListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inquiries/new"
            element={
              <ProtectedRoute>
                <InquiryCreatePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inquiries/:inquiryId"
            element={
              <ProtectedRoute>
                <InquiryDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/coupons"
            element={
              <ProtectedRoute>
                <CouponListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/coupons/my"
            element={
              <ProtectedRoute>
                <MyCouponsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminLayout />
              </AdminRoute>
            }
          >
            <Route index element={<AdminDashboardPage />} />
            <Route path="products" element={<AdminProductListPage />} />
            <Route path="products/new" element={<AdminProductFormPage />} />
            <Route path="products/:productId/edit" element={<AdminProductFormPage />} />
            <Route path="orders" element={<AdminOrderListPage />} />
            <Route path="inquiries" element={<AdminInquiryListPage />} />
          </Route>
        </Routes>
        </Suspense>
      </main>
    </>
  )
}

export default App
