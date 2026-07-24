import { useEffect } from 'react'
import { Route, Routes } from 'react-router-dom'
import { Header } from './components/Header'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { AdminLayout } from './components/AdminLayout'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { ProductListPage } from './pages/ProductListPage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { OrderListPage } from './pages/OrderListPage'
import { OrderDetailPage } from './pages/OrderDetailPage'
import { ReviewFormPage } from './pages/ReviewFormPage'
import { MyReviewsPage } from './pages/MyReviewsPage'
import { InquiryListPage } from './pages/InquiryListPage'
import { InquiryCreatePage } from './pages/InquiryCreatePage'
import { InquiryDetailPage } from './pages/InquiryDetailPage'
import { CouponListPage } from './pages/CouponListPage'
import { MyCouponsPage } from './pages/MyCouponsPage'
import { AdditionalInfoPage } from './pages/AdditionalInfoPage'
import { MyPage } from './pages/MyPage'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage'
import { AdminProductListPage } from './pages/admin/AdminProductListPage'
import { AdminProductFormPage } from './pages/admin/AdminProductFormPage'
import { AdminOrderListPage } from './pages/admin/AdminOrderListPage'
import { AdminInquiryListPage } from './pages/admin/AdminInquiryListPage'
import { trackVisitOnce } from './api/track'

function App() {
  useEffect(() => {
    trackVisitOnce()
  }, [])

  return (
    <>
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<ProductListPage />} />
          <Route path="/products/:productId" element={<ProductDetailPage />} />
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
      </main>
    </>
  )
}

export default App
