import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/user'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import type { LoginForm } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshTokenValue = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || '')
  const role = computed(() => userInfo.value?.role || '')
  const permissions = computed(() => userInfo.value?.permissions || [])

  function hasPermission(perm: string) {
    if (permissions.value.includes('*')) return true
    return permissions.value.includes(perm)
  }

  async function login(form: LoginForm) {
    const res = await loginApi(form)
    token.value = res.data.accessToken
    refreshTokenValue.value = res.data.refreshToken
    localStorage.setItem('token', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
    } catch {
      logout()
    }
  }

  async function initOnAppStart() {
    if (initialized.value) return
    if (token.value && !userInfo.value) {
      await fetchUserInfo()
    }
    initialized.value = true
  }

  function logout() {
    const t = token.value
    const r = refreshTokenValue.value
    if (t || r) {
      logoutApi(t, r).catch((error) => console.warn('服务端会话注销失败', error))
    }
    token.value = ''
    refreshTokenValue.value = ''
    userInfo.value = null
    initialized.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  return { token, refreshToken: refreshTokenValue, userInfo, initialized, isLoggedIn, username, realName, role, permissions, hasPermission, login, fetchUserInfo, initOnAppStart, logout }
})
