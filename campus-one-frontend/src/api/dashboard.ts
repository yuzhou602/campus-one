import request from '@/utils/request'

export function getStudentDashboard() {
  return request.get('/dashboard/student')
}

export function getTeacherDashboard() {
  return request.get('/dashboard/teacher')
}

export function getAdminDashboard() {
  return request.get('/dashboard/admin')
}

export function getAnalyticsOverview() {
  return request.get('/analytics/overview')
}

export function getVenueAnalytics() {
  return request.get('/analytics/venue')
}

export function getRepairAnalytics() {
  return request.get('/analytics/repair')
}
