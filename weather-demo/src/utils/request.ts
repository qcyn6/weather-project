import axios, { type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'weather_token'
const USER_KEY = 'weather_user'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

instance.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    // 文件下载（blob）直接透传
    if (response.data instanceof Blob) {
      return response.data as any
    }
    const { code, msg, data } = response.data
    if (code === 200) {
      return data as any
    }
    if (code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      ElMessage.error(msg || '登录已过期，请重新登录')
      location.href = '/login'
      return Promise.reject(new Error(msg))
    }
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg))
  },
  error => {
    ElMessage.error('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return instance.get(url, { params }) as Promise<T>
}

export function getWithTimeout<T>(url: string, params: Record<string, unknown> | undefined, timeoutMs: number): Promise<T> {
  return instance.get(url, { params, timeout: timeoutMs }) as Promise<T>
}

export function post<T>(url: string, data?: unknown): Promise<T> {
  return instance.post(url, data) as Promise<T>
}

export function postWithTimeout<T>(
  url: string,
  data: unknown,
  params: Record<string, unknown> | undefined,
  timeoutMs: number
): Promise<T> {
  return instance.post(url, data, { params, timeout: timeoutMs }) as Promise<T>
}

/** 下载文件（xlsx 等 blob），fileName 由调用方按已知参数拼出 */
export async function download(url: string, params: Record<string, unknown>, fileName: string): Promise<void> {
  const blob = (await instance.get(url, { params, responseType: 'blob' })) as Blob
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName
  link.click()
  URL.revokeObjectURL(link.href)
}

export default instance
