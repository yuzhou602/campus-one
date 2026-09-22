import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/notice'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const pollingTimer = ref<ReturnType<typeof setInterval> | null>(null)

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data
    } catch {}
  }

  function startPolling() {
    fetchUnreadCount()
    pollingTimer.value = setInterval(fetchUnreadCount, 30000)
  }

  function stopPolling() {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value)
      pollingTimer.value = null
    }
  }

  return { unreadCount, fetchUnreadCount, startPolling, stopPolling }
})
