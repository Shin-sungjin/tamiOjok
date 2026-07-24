import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as adminApi from '../../api/admin'
import { getProduct } from '../../api/products'
import { extractErrorMessage } from '../../api/errors'

export function AdminProductFormPage() {
  const { productId } = useParams<{ productId: string }>()
  const isEdit = Boolean(productId)
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [description, setDescription] = useState('')
  const [initialStock, setInitialStock] = useState('0')
  const [imageUrlsText, setImageUrlsText] = useState('')
  const [activeImage, setActiveImage] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (!isEdit || !productId) return
    getProduct(Number(productId))
      .then((product) => {
        setName(product.name)
        setPrice(String(product.price))
        setDescription(product.description ?? '')
        setImageUrlsText(product.imageUrls.join('\n'))
      })
      .catch((err) => setError(extractErrorMessage(err)))
  }, [isEdit, productId])

  const images = imageUrlsText
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
  const activeUrl = images[activeImage] ?? images[0]

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      if (isEdit && productId) {
        await adminApi.updateProduct(Number(productId), {
          name,
          price: Number(price),
          description: description || undefined,
          imageUrls: images,
        })
      } else {
        await adminApi.createProduct({
          name,
          price: Number(price),
          description: description || undefined,
          initialStock: Number(initialStock),
          imageUrls: images,
        })
      }
      navigate('/admin/products')
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="page">
      <Link to="/admin/products">← 상품 목록으로</Link>

      <div className="product-detail admin-product-form">
        <div className="product-detail__gallery">
          <div className="product-detail__main-image">
            {activeUrl ? (
              <img src={activeUrl} alt={name || '상품 이미지 미리보기'} />
            ) : (
              <span className="product-card__image-placeholder">이미지 미리보기</span>
            )}
          </div>
          {images.length > 1 && (
            <div className="product-detail__thumbs">
              {images.map((url, index) => (
                <button
                  key={url + index}
                  type="button"
                  className={
                    'product-detail__thumb' + (index === activeImage ? ' product-detail__thumb--active' : '')
                  }
                  onClick={() => setActiveImage(index)}
                >
                  <img src={url} alt={`이미지 ${index + 1}`} />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="product-detail__info">
          <h1>{isEdit ? '상품 수정' : '새 상품 등록'}</h1>
          <form className="admin-product-form__fields" onSubmit={handleSubmit}>
            <label>
              상품명
              <input value={name} onChange={(e) => setName(e.target.value)} required />
            </label>
            <label>
              가격
              <input type="number" min={1} value={price} onChange={(e) => setPrice(e.target.value)} required />
            </label>
            <label>
              설명
              <textarea rows={5} value={description} onChange={(e) => setDescription(e.target.value)} />
            </label>
            <label>
              이미지 URL (한 줄에 하나씩, 첫 줄이 대표 이미지)
              <textarea
                rows={4}
                placeholder={'https://example.com/image1.jpg\nhttps://example.com/image2.jpg'}
                value={imageUrlsText}
                onChange={(e) => {
                  setImageUrlsText(e.target.value)
                  setActiveImage(0)
                }}
              />
            </label>
            {!isEdit && (
              <label>
                초기 재고
                <input
                  type="number"
                  min={0}
                  value={initialStock}
                  onChange={(e) => setInitialStock(e.target.value)}
                  required
                />
              </label>
            )}
            {error && <p className="form-error">{error}</p>}
            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? '저장 중...' : '저장'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
