import { defineStore } from 'pinia'
import { ref } from 'vue'
import { messageApi } from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const unread = ref(0)
  let timer: number | undefined

  async function refresh() {
    try {
      unread.value = await messageApi.unreadCount()
    } catch {
      // 静默失败：未登录或网络抖动都不需要打扰用户
    }
  }

  function startPolling(intervalMs = 30_000) {
    stopPolling()
    refresh()
    timer = window.setInterval(refresh, intervalMs)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = undefined
    }
  }

  function reset() {
    unread.value = 0
  }

  return { unread, refresh, startPolling, stopPolling, reset }
})
