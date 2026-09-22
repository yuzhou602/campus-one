import type { AxiosAdapter, AxiosResponse } from 'axios'

export const isDemoMode = import.meta.env.VITE_DEMO_MODE === 'true'

type DemoUser = {
  id: number
  username: string
  realName: string
  role: string
  roleName: string
  permissions: string[]
  dataScope: string
  email: string
  phone: string
  status: number
  createdAt: string
}

const users: DemoUser[] = [
  { id: 1, username: 'admin', realName: '系统管理员', role: 'SUPER_ADMIN', roleName: '超级管理员', permissions: ['*'], dataScope: 'ALL', email: 'admin@campus.example', phone: '13800000001', status: 1, createdAt: '2026-03-01 09:00' },
  { id: 2, username: 'teacher01', realName: '陈老师', role: 'TEACHER', roleName: '教师', permissions: ['application:approve'], dataScope: 'DEPARTMENT', email: 'teacher01@campus.example', phone: '13800000002', status: 1, createdAt: '2026-03-02 09:00' },
  { id: 3, username: 'counselor01', realName: '林辅导员', role: 'COUNSELOR', roleName: '辅导员', permissions: ['application:approve'], dataScope: 'COLLEGE', email: 'counselor01@campus.example', phone: '13800000003', status: 1, createdAt: '2026-03-02 10:00' },
  { id: 4, username: 'student01', realName: '周同学', role: 'STUDENT', roleName: '学生', permissions: ['application:create'], dataScope: 'SELF', email: 'student01@campus.example', phone: '13800000004', status: 1, createdAt: '2026-03-03 09:00' },
  { id: 5, username: 'student02', realName: '许同学', role: 'STUDENT', roleName: '学生', permissions: ['application:create'], dataScope: 'SELF', email: 'student02@campus.example', phone: '13800000005', status: 1, createdAt: '2026-03-03 10:00' },
  { id: 6, username: 'service01', realName: '李师傅', role: 'SERVICE', roleName: '服务人员', permissions: ['repair:handle'], dataScope: 'DEPARTMENT', email: 'service01@campus.example', phone: '13800000006', status: 1, createdAt: '2026-03-04 09:00' },
]

const services = [
  { id: 1, name: '请假申请', description: '课程、实习与日常请假在线登记', audience: '全体学生', duration: '2 个工作日', approvalFlow: '辅导员 → 学院' },
  { id: 2, name: '学生证明申请', description: '在读证明、成绩证明等材料申请', audience: '全体学生', duration: '1 个工作日', approvalFlow: '教务审核' },
  { id: 3, name: '场地特殊使用', description: '常规预约时段以外的场地使用申请', audience: '师生', duration: '3 个工作日', approvalFlow: '场馆 → 保卫处' },
  { id: 4, name: '活动场地申请', description: '社团和班级活动场地备案', audience: '学生组织', duration: '3 个工作日', approvalFlow: '指导教师 → 团委' },
  { id: 5, name: '物品借用申请', description: '公共器材和活动物资借用', audience: '师生', duration: '1 个工作日', approvalFlow: '资产管理员' },
  { id: 6, name: '宿舍事务申请', description: '调宿、晚归等宿舍事务登记', audience: '住宿学生', duration: '2 个工作日', approvalFlow: '辅导员 → 宿管' },
]

const resources = [
  { id: 1, resourceCode: 'ROOM-A301', resourceName: '智慧研讨室 A301', resourceType: 'meeting', buildingId: 2, buildingName: '教学楼 A', roomNumber: 'A301', capacity: 24, equipmentJson: '投影、白板、视频会议', status: 'AVAILABLE', needApproval: false, availableSlots: ['08:00-10:00', '10:00-12:00 已占用', '14:00-16:00'] },
  { id: 2, resourceCode: 'ROOM-I205', resourceName: '多媒体教室 I205', resourceType: 'classroom', buildingId: 1, buildingName: '信息楼', roomNumber: 'I205', capacity: 60, equipmentJson: '电脑、投影、扩声系统', status: 'AVAILABLE', needApproval: true, availableSlots: ['08:00-10:00', '14:00-16:00', '16:00-18:00 已占用'] },
  { id: 3, resourceCode: 'LAB-402', resourceName: '创新实验室 402', resourceType: 'lab', buildingId: 3, buildingName: '实验楼', roomNumber: '402', capacity: 36, equipmentJson: '实验台、示波器、工具柜', status: 'AVAILABLE', needApproval: true, availableSlots: ['10:00-12:00', '14:00-16:00 已占用', '18:00-20:00'] },
  { id: 4, resourceCode: 'STUDY-B1', resourceName: '共享自习室 B1', resourceType: 'study', buildingId: 1, buildingName: '图书馆', roomNumber: 'B1', capacity: 80, equipmentJson: '插座、无线网络、储物柜', status: 'AVAILABLE', needApproval: false, availableSlots: ['08:00-10:00', '10:00-12:00', '14:00-16:00'] },
  { id: 5, resourceCode: 'SPORT-01', resourceName: '室内羽毛球馆', resourceType: 'badminton', buildingId: 3, buildingName: '体育馆', roomNumber: '1F', capacity: 40, equipmentJson: '标准球网、更衣室', status: 'AVAILABLE', needApproval: false, availableSlots: ['10:00-12:00 已占用', '14:00-16:00', '18:00-20:00'] },
]

const activities = [
  { id: 1, activityNo: 'ACT-2026-091', title: '人工智能与校园创新论坛', description: '来自高校与产业界的嘉宾共同分享人工智能在教育、科研和校园治理中的实践。', category: 'academic', categoryName: '学术', location: '大学生活动中心报告厅', startTime: '2026-10-12 14:00', endTime: '2026-10-12 17:00', organizer: '信息工程学院', capacity: 300, maxCount: 300, registered: 186, registeredCount: 186, enrolledCount: 186, status: 'ACTIVE' },
  { id: 2, activityNo: 'ACT-2026-096', title: '秋季社团开放日', description: '四十余个学生社团集中展示，欢迎发现新的兴趣与伙伴。', category: 'club', categoryName: '社团', location: '中心广场', startTime: '2026-10-18 09:00', endTime: '2026-10-18 16:00', organizer: '校团委', capacity: 800, maxCount: 800, registered: 432, registeredCount: 432, enrolledCount: 432, status: 'ACTIVE' },
  { id: 3, activityNo: 'ACT-2026-103', title: '校园夜跑公益挑战', description: '以运动里程兑换公益物资，在秋夜里为乡村学校点亮一份支持。', category: 'sports', categoryName: '体育', location: '东区田径场', startTime: '2026-10-25 19:00', endTime: '2026-10-25 21:00', organizer: '体育部 · 青协', capacity: 500, maxCount: 500, registered: 275, registeredCount: 275, enrolledCount: 275, status: 'ACTIVE' },
]

const notices = [
  { id: 1, title: '关于国庆假期校园服务安排的通知', summary: '假期期间图书馆、食堂和校车运行时间有所调整。', content: '国庆假期期间，图书馆开放时间为 08:30—21:00；各食堂错峰开放；校车按照假期时刻表运行。请师生合理安排时间。', source: '学校办公室', senderName: '学校办公室', time: '09-20 10:30', createdAt: '2026-09-20 10:30', important: true, unread: true, isRead: false, type: 'school' },
  { id: 2, title: '秋季学期奖学金评审工作开始', summary: '请符合条件的同学在本周五前完成材料提交。', content: '本学期奖学金评审现已启动。请登录办事大厅填写申请，并在规定时间内提交证明材料。', source: '学生工作处', senderName: '学生工作处', time: '09-19 16:20', createdAt: '2026-09-19 16:20', important: true, unread: false, isRead: true, type: 'school' },
  { id: 3, title: '信息楼网络维护公告', summary: '周六凌晨将进行网络设备升级。', content: '信息楼网络将于本周六 00:00—04:00 进行升级维护，期间可能短时中断。', source: '信息化中心', senderName: '信息化中心', time: '09-18 09:00', createdAt: '2026-09-18 09:00', important: false, unread: true, isRead: false, type: 'system' },
  { id: 4, title: '图书馆新生入馆培训开放预约', summary: '线上与线下培训场次均已开放。', content: '新生可选择线上学习或线下讲解场次，完成培训后将开通完整借阅权限。', source: '图书馆', senderName: '图书馆', time: '09-16 14:00', createdAt: '2026-09-16 14:00', important: false, unread: false, isRead: true, type: 'school' },
]

const initialApplications: Array<Record<string, any>> = [
  { id: 1, applicationNo: 'APP-20260918-001', serviceId: 1, serviceName: '请假申请', title: '课程请假申请', applicantId: 4, applicantName: '周同学', studentNo: '2026001001', department: '信息工程学院', className: '软件工程 2601 班', leaveType: '事假', startTime: '2026-09-24 08:00', endTime: '2026-09-24 18:00', phone: '13800000004', reason: '参加校外创新实践活动。', status: 'PENDING', currentNode: '辅导员审批', urgeCount: 0, submittedAt: '2026-09-18 10:20', createdAt: '2026-09-18 10:20' },
  { id: 2, applicationNo: 'APP-20260916-004', serviceId: 2, serviceName: '学生证明申请', title: '在读证明申请', applicantId: 5, applicantName: '许同学', studentNo: '2026001002', department: '外国语学院', className: '英语 2601 班', reason: '办理交流项目材料。', status: 'APPROVED', currentNode: '已归档', urgeCount: 0, submittedAt: '2026-09-16 09:10', completedAt: '2026-09-17 15:20', createdAt: '2026-09-16 09:10' },
  { id: 3, applicationNo: 'APP-20260914-008', serviceId: 4, serviceName: '活动场地申请', title: '社团迎新活动场地申请', applicantId: 4, applicantName: '周同学', department: '信息工程学院', reason: '用于社团迎新交流活动。', status: 'REJECTED', currentNode: '已退回', urgeCount: 0, submittedAt: '2026-09-14 11:30', completedAt: '2026-09-15 10:00', createdAt: '2026-09-14 11:30' },
]

const initialRepairs: Array<Record<string, any>> = [
  { id: 1, repairNo: 'FIX-20260920-021', userId: 4, userName: '周同学', title: '宿舍空调无法制冷', location: '学生宿舍 12 号楼 301', category: 'air-conditioner', description: '空调可启动但运行半小时仍无冷风。', contact: '13800000004', availableTime: '工作日 18:00 后', priority: 'NORMAL', urgency: '普通', status: 'PROCESSING', createdAt: '2026-09-20 09:20' },
  { id: 2, repairNo: 'FIX-20260919-016', userId: 4, userName: '周同学', title: '走廊照明故障', location: '信息楼 3 层东侧', category: 'electric', description: '两盏走廊灯持续闪烁。', contact: '13800000004', availableTime: '全天', priority: 'NORMAL', urgency: '普通', status: 'ASSIGNED', createdAt: '2026-09-19 16:40' },
  { id: 3, repairNo: 'FIX-20260912-003', userId: 5, userName: '许同学', title: '洗手间水龙头漏水', location: '教学楼 A 2 层', category: 'water', description: '水龙头关闭后仍持续滴水。', contact: '13800000005', availableTime: '工作日白天', priority: 'LOW', urgency: '一般', status: 'RESOLVED', createdAt: '2026-09-12 11:10' },
]

const initialReservations: Array<Record<string, any>> = [
  { id: 1, reservationNo: 'RES-20260925-008', resourceId: 1, resourceName: '智慧研讨室 A301', userId: 4, userName: '周同学', reservationDate: '2026-09-25', startTime: '14:00', endTime: '16:00', purpose: '课程小组讨论', participantCount: 8, status: 'CONFIRMED', createdAt: '2026-09-20 14:00' },
  { id: 2, reservationNo: 'RES-20260928-011', resourceId: 5, resourceName: '室内羽毛球馆', userId: 4, userName: '周同学', reservationDate: '2026-09-28', startTime: '18:00', endTime: '20:00', purpose: '班级体育活动', participantCount: 16, status: 'PENDING', createdAt: '2026-09-21 09:30' },
]

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function loadState<T>(key: string, fallback: T): T {
  try {
    const value = localStorage.getItem(`campusone-demo-${key}`)
    return value ? JSON.parse(value) as T : clone(fallback)
  } catch {
    return clone(fallback)
  }
}

function saveState(key: string, value: unknown) {
  localStorage.setItem(`campusone-demo-${key}`, JSON.stringify(value))
}

let applications = loadState('applications', initialApplications)
let repairs = loadState('repairs', initialRepairs)
let reservations = loadState('reservations', initialReservations)

function bodyOf(data: unknown): Record<string, any> {
  if (!data) return {}
  if (typeof data === 'string') {
    try { return JSON.parse(data) as Record<string, any> } catch { return {} }
  }
  return data as Record<string, any>
}

function page<T>(records: T[], params: any = {}) {
  const current = Number(params.page || params.current || 1)
  const size = Number(params.pageSize || params.size || 20)
  return { records: records.slice((current - 1) * size, current * size), total: records.length, current, size, page: current, pageSize: size }
}

function currentUser() {
  const username = localStorage.getItem('campusone-demo-current-user') || 'student01'
  return users.find(user => user.username === username) || users[3]
}

function success(data: unknown, config: any): AxiosResponse {
  return {
    data: { code: 200, message: '操作成功', success: true, data, timestamp: Date.now() },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  }
}

function detailId(path: string) {
  return Number(path.match(/\/(\d+)(?:\/|$)/)?.[1])
}

export const demoAdapter: AxiosAdapter = async config => {
  const method = (config.method || 'get').toLowerCase()
  const path = `/${String(config.url || '').replace(/^\/+/, '').replace(/^api\/v1\//, '')}`
  const body = bodyOf(config.data)
  const params: any = config.params || {}
  let result: unknown = null

  if (path === '/auth/login' && method === 'post') {
    const user = users.find(item => item.username === body.username)
    if (!user || body.password !== 'demo123') throw new Error('演示账号或密码不正确，请使用页面上的快捷账号')
    localStorage.setItem('campusone-demo-current-user', user.username)
    result = { accessToken: `demo-token-${user.username}`, refreshToken: `demo-refresh-${user.username}`, expiresIn: 86400, userInfo: user }
  } else if (path === '/auth/me') {
    result = currentUser()
  } else if (path === '/auth/refresh') {
    const user = currentUser()
    result = { accessToken: `demo-token-${user.username}`, refreshToken: `demo-refresh-${user.username}`, expiresIn: 86400 }
  } else if (path === '/auth/logout') {
    result = true
  } else if (path === '/services') {
    result = services
  } else if (path === '/applications/my') {
    result = page(applications, params)
  } else if (path === '/applications' && method === 'post') {
    const user = currentUser()
    const item = { id: Date.now(), applicationNo: `APP-DEMO-${String(applications.length + 1).padStart(3, '0')}`, serviceId: body.serviceId, serviceName: body.title, title: body.title, applicantId: user.id, applicantName: user.realName, reason: JSON.parse(body.formData || '{}').reason || '在线演示申请', status: 'PENDING', currentNode: '辅导员审批', urgeCount: 0, submittedAt: new Date().toISOString(), createdAt: new Date().toISOString() }
    applications.unshift(item)
    saveState('applications', applications)
    result = item
  } else if (path === '/applications/approvals/pending/count') {
    result = applications.filter(item => item.status === 'PENDING').length
  } else if (path === '/applications/approvals/pending') {
    result = page(applications.filter(item => item.status === 'PENDING'), params)
  } else if (path === '/applications/approvals/processed') {
    result = page(applications.filter(item => item.status !== 'PENDING'), params)
  } else if (/^\/applications\/approvals\/\d+\/(approve|reject)$/.test(path)) {
    const id = detailId(path)
    const item = applications.find(entry => entry.id === id)
    if (item) {
      item.status = path.endsWith('/approve') ? 'APPROVED' : 'REJECTED'
      item.currentNode = '已归档'
      saveState('applications', applications)
    }
    result = item
  } else if (/^\/applications\/\d+\/approvals$/.test(path)) {
    result = [{ id: 1, nodeName: '提交申请', assigneeName: '系统', action: 'APPROVED', comment: '材料已登记', createdAt: '2026-09-18 10:20' }]
  } else if (/^\/applications\/\d+\/rollback$/.test(path)) {
    const item = applications.find(entry => entry.id === detailId(path))
    if (item) item.currentNode = '申请人补充材料'
    saveState('applications', applications)
    result = item
  } else if (/^\/applications\/\d+\/urge$/.test(path)) {
    const item = applications.find(entry => entry.id === detailId(path))
    if (item) item.urgeCount = (item.urgeCount || 0) + 1
    saveState('applications', applications)
    result = true
  } else if (/^\/applications\/\d+$/.test(path)) {
    result = applications.find(item => item.id === detailId(path)) || applications[0]
  } else if (path === '/reservations/resources') {
    let filtered = resources
    if (params.resourceType) filtered = filtered.filter(item => item.resourceType === params.resourceType)
    if (params.buildingId) filtered = filtered.filter(item => item.buildingId === Number(params.buildingId))
    result = page(filtered, params)
  } else if (/^\/resources\/\d+$/.test(path)) {
    result = resources.find(item => item.id === detailId(path)) || resources[0]
  } else if (path === '/reservations/availability') {
    result = [
      { startTime: '08:00', endTime: '10:00', available: true },
      { startTime: '10:00', endTime: '12:00', available: false, reservationId: 99 },
      { startTime: '14:00', endTime: '16:00', available: true },
      { startTime: '16:00', endTime: '18:00', available: true },
      { startTime: '18:00', endTime: '20:00', available: false, reservationId: 100 },
    ]
  } else if (path === '/reservations/my') {
    result = page(reservations, params)
  } else if (path === '/reservations' && method === 'post') {
    const resource = resources.find(item => item.id === Number(body.resourceId)) || resources[0]
    const user = currentUser()
    const item = { id: Date.now(), reservationNo: `RES-DEMO-${reservations.length + 1}`, resourceId: resource.id, resourceName: resource.resourceName, userId: user.id, userName: user.realName, reservationDate: body.reservationDate, startTime: body.startTime, endTime: body.endTime, purpose: body.purpose, participantCount: body.attendeeCount, status: 'CONFIRMED', createdAt: new Date().toISOString() }
    reservations.unshift(item)
    saveState('reservations', reservations)
    result = item
  } else if (/^\/reservations\/\d+\/cancel$/.test(path)) {
    const item = reservations.find(entry => entry.id === detailId(path))
    if (item) item.status = 'CANCELLED'
    saveState('reservations', reservations)
    result = item
  } else if (/^\/reservations\/\d+$/.test(path)) {
    result = reservations.find(item => item.id === detailId(path)) || reservations[0]
  } else if (path === '/repairs/my' || path === '/repairs/assigned') {
    result = page(repairs, params)
  } else if (path === '/repairs' && method === 'post') {
    const user = currentUser()
    const item = { id: Date.now(), repairNo: `FIX-DEMO-${repairs.length + 1}`, userId: user.id, userName: user.realName, title: body.description?.slice(0, 18) || '新报修工单', location: body.location, category: body.category, description: body.description, contact: body.contact, availableTime: body.availableTime, priority: 'NORMAL', urgency: '普通', status: 'SUBMITTED', createdAt: new Date().toISOString() }
    repairs.unshift(item)
    saveState('repairs', repairs)
    result = item
  } else if (/^\/repairs\/\d+\/status$/.test(path)) {
    const item = repairs.find(entry => entry.id === detailId(path))
    if (item) item.status = body.status
    saveState('repairs', repairs)
    result = item
  } else if (/^\/repairs\/\d+\/assign$/.test(path)) {
    result = true
  } else if (/^\/repairs\/\d+$/.test(path)) {
    result = repairs.find(item => item.id === detailId(path)) || repairs[0]
  } else if (path === '/activities/registrations/my') {
    result = page(activities.slice(0, 2), params)
  } else if (path === '/activities') {
    result = page(activities, params)
  } else if (/^\/activities\/\d+\/register$/.test(path) && method === 'delete') {
    result = true
  } else if (/^\/activities\/\d+\/(register|checkin)$/.test(path)) {
    const item = activities.find(entry => entry.id === detailId(path))
    if (item && path.endsWith('/register')) {
      item.registered += 1
      item.registeredCount += 1
      item.enrolledCount += 1
    }
    result = true
  } else if (/^\/activities\/\d+$/.test(path)) {
    result = activities.find(item => item.id === detailId(path)) || activities[0]
  } else if (path === '/notices') {
    result = page(notices, params)
  } else if (path === '/notifications/unread-count') {
    result = notices.filter(item => !item.isRead).length
  } else if (path === '/notifications/my') {
    result = notices
  } else if (path === '/notifications/read-all') {
    notices.forEach(item => { item.isRead = true; item.unread = false })
    result = true
  } else if (/^\/notifications\/\d+\/read$/.test(path)) {
    const item = notices.find(entry => entry.id === detailId(path))
    if (item) { item.isRead = true; item.unread = false }
    result = true
  } else if (/^\/notifications\/\d+$/.test(path) && method === 'delete') {
    result = true
  } else if (/^\/notifications\/\d+$/.test(path)) {
    result = notices.find(item => item.id === detailId(path)) || notices[0]
  } else if (path === '/dashboard/student') {
    result = { recentActivities: [{ id: 1, name: '软件工程', startTime: '08:00', endTime: '09:40', location: '信息楼 I205', teacher: '陈老师', week: 4 }, { id: 2, name: '大学英语', startTime: '14:00', endTime: '15:40', location: '教学楼 A102', teacher: '王老师', week: 4 }], pendingRepairs: 1, upcomingReservations: 2 }
  } else if (path === '/dashboard/teacher') {
    result = { recentActivities: [], pendingRepairs: 2, upcomingReservations: 1 }
  } else if (path === '/dashboard/admin' || path === '/analytics/overview') {
    result = { totalUsers: 12846, todayReservations: 74, pendingRepairs: 19, totalActivities: 126 }
  } else if (path === '/analytics/venue') {
    result = { totalReservations: 2386, todayReservations: 74, avgDaily: 63 }
  } else if (path === '/analytics/repair') {
    result = { accepted: 21, submitted: 13, resolved: 86 }
  } else if (path === '/users') {
    const keyword = String(params.keyword || '').toLowerCase()
    const filtered = keyword ? users.filter(item => `${item.username}${item.realName}`.toLowerCase().includes(keyword)) : users
    result = page(filtered, params)
  } else if (path === '/system/info') {
    result = { name: 'CampusOne 在线演示', version: '1.0.0-demo', javaVersion: 'Static Demo', osName: 'GitHub Pages' }
  } else if (path === '/tasks/my') {
    result = { pendingApprovals: page(applications.filter(item => item.status === 'PENDING'), params), myReservations: page(reservations, params), myRepairs: page(repairs.filter(item => item.status !== 'RESOLVED'), params) }
  } else if (path === '/ai/quick-actions') {
    result = ['查课表', '找教室', '查申请', '校园活动']
  } else if (path === '/ai/knowledge') {
    result = page([], params)
  }

  await new Promise(resolve => window.setTimeout(resolve, 120))
  return success(result, config)
}

export function getDemoAiReply(question: string) {
  if (/课表|上课/.test(question)) return '你今天有 **2 节课**：\n\n- 08:00 软件工程 · 信息楼 I205\n- 14:00 大学英语 · 教学楼 A102\n\n这是在线演示数据，可在「智慧首页」查看。'
  if (/教室|场地|预约/.test(question)) return '目前推荐 **智慧研讨室 A301**，可容纳 24 人，14:00—16:00 可预约。你可以前往「场地预约」完成演示操作。'
  if (/申请|审批/.test(question)) return '当前有 1 份请假申请正在「辅导员审批」节点。你可以前往「我的申请」查看进度，或切换教师账号体验审批。'
  if (/报修/.test(question)) return '你有 1 个处理中报修：宿舍空调无法制冷。维修人员已接单，当前状态为「处理中」。'
  if (/活动/.test(question)) return '近期推荐：人工智能与校园创新论坛、秋季社团开放日、校园夜跑公益挑战。前往「校园活动」可以查看详情并体验报名。'
  return `这是 CampusOne 的在线演示助手。关于“${question}”，你可以从左侧目录进入事务、预约、报修、活动等模块体验；演示操作只保存在当前浏览器。`
}
