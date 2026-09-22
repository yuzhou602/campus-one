import request from '@/utils/request'

export function getUserList(params?: any) {
  return request.get('/users', { params })
}

export function getSystemInfo() {
  return request.get('/system/info')
}