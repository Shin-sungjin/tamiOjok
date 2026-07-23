import { AxiosError } from 'axios'
import type { ErrorResponse } from './types'

// 백엔드 GlobalExceptionHandler가 내려주는 { code, message, errors } 형식을
// 사람이 읽을 수 있는 한 줄 메시지로 변환합니다.
export function extractErrorMessage(error: unknown): string {
  if (error instanceof AxiosError && error.response) {
    const body = error.response.data as ErrorResponse | undefined
    if (body?.errors?.length) {
      return body.errors.join(', ')
    }
    if (body?.message) {
      return body.message
    }
  }
  return '요청 처리 중 오류가 발생했습니다.'
}
