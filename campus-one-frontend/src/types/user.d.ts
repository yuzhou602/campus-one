export interface UserInfo {
  id: number
  username: string
  realName: string
  avatar?: string
  email?: string
  phone?: string
  role: string
  roleName: string
  permissions: string[]
  dataScope: string
  collegeId?: number
  collegeName?: string
  majorId?: number
  majorName?: string
  classId?: number
  className?: string
}

export interface LoginForm {
  username: string
  password: string
  captcha?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  userInfo: UserInfo
}
