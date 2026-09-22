import request from '@/utils/request'
import type { LoginForm, LoginResult } from '@/types/user'
import type { ApiResponse } from '@/types/api'

export function login(data: LoginForm) {
  return request.post<any, ApiResponse<LoginResult>>('/auth/login', data)
}

export function logout(accessToken?: string, refreshToken?: string) {
  return request.post('/auth/logout', { accessToken, refreshToken })
}

export function getUserInfo() {
  return request.get('/auth/me')
}

export function refreshToken(refreshToken: string) {
  return request.post('/auth/refresh', { refreshToken })
}
