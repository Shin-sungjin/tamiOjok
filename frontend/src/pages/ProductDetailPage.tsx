import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProduct } from '../api/products'
import { extractErrorMessage } from '../api/errors'
import type { ProductResponse } from '../api/types'

export function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>()
  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!productId) return
    getProduct(Number(productId))
      .then(setProduct)
      .catch((err) => setError(extractErrorMessage(err)))
  }, [productId])

  if (error) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
        <Link to="/">목록으로 돌아가기</Link>
      </div>
    )
  }

  if (!product) {
    return <p className="page">불러오는 중...</p>
  }

  return (
    <div className="page">
      <Link to="/">← 목록으로</Link>
      <h1>{product.name}</h1>
      <p className="product-detail__price">{product.price.toLocaleString()}원</p>
      <p>{product.description ?? '상품 설명이 없습니다.'}</p>
      <p>재고: {product.availableStock}개</p>
      <p>판매 상태: {product.status}</p>
      {/* TODO: 장바구니 담기 버튼은 cart API 연동 시 추가 */}
    </div>
  )
}
