import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as usersApi from '../api/users'
import { extractErrorMessage } from '../api/errors'

export function AdditionalInfoPage() {
  const { refreshUser } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    phoneNumber: '',
    recipientName: '',
    recipientPhone: '',
    zipcode: '',
    addressMain: '',
    addressDetail: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField(field: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await usersApi.completeAdditionalInfo({
        ...form,
        addressDetail: form.addressDetail || undefined,
      })
      await refreshUser()
      navigate('/')
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="form-page">
      <h1>추가 정보 입력</h1>
      <p>소셜 로그인 계정에 연락처와 기본 배송지를 등록해주세요. 등록 전까지 주문/결제를 이용할 수 없습니다.</p>
      <form onSubmit={handleSubmit}>
        <label>
          휴대폰번호
          <input
            type="tel"
            value={form.phoneNumber}
            onChange={(e) => updateField('phoneNumber', e.target.value)}
            placeholder="010-0000-0000"
            required
          />
        </label>
        <label>
          받는 사람
          <input
            type="text"
            value={form.recipientName}
            onChange={(e) => updateField('recipientName', e.target.value)}
            required
          />
        </label>
        <label>
          받는 사람 연락처
          <input
            type="tel"
            value={form.recipientPhone}
            onChange={(e) => updateField('recipientPhone', e.target.value)}
            required
          />
        </label>
        <label>
          우편번호
          <input
            type="text"
            value={form.zipcode}
            onChange={(e) => updateField('zipcode', e.target.value)}
            required
          />
        </label>
        <label>
          주소
          <input
            type="text"
            value={form.addressMain}
            onChange={(e) => updateField('addressMain', e.target.value)}
            required
          />
        </label>
        <label>
          상세 주소 (선택)
          <input
            type="text"
            value={form.addressDetail}
            onChange={(e) => updateField('addressDetail', e.target.value)}
          />
        </label>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '저장 중...' : '저장하고 시작하기'}
        </button>
      </form>
    </div>
  )
}
