import request from '@/utils/request'

export function getActivities(params?: any) {
  return request.get('/activities', { params })
}

export function getActivityById(id: number) {
  return request.get(`/activities/${id}`)
}

export function registerActivity(id: number) {
  return request.post(`/activities/${id}/register`)
}

export function cancelRegistration(id: number) {
  return request.delete(`/activities/${id}/register`)
}

export function checkInActivity(id: number) {
  return request.post(`/activities/${id}/checkin`)
}

export function getMyRegistrations(params?: any) {
  return request.get('/activities/registrations/my', { params })
}
