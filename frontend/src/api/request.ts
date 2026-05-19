import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const user = useUserStore()
  if (user.token) {
    config.headers.set('Authorization', `Bearer ${user.token}`)
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResult
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data as unknown
      if (body.code === 401) {
        useUserStore().logout()
        router.push({ name: 'login' })
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return resp.data
  },
  (err) => {
    if (err.response?.status === 401) {
      useUserStore().logout()
      router.push({ name: 'login' })
    }
    ElMessage.error(err.response?.data?.message || err.message || '网络异常')
    return Promise.reject(err)
  },
)

export default request
