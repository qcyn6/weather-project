import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { loginApi, type LoginResult } from '@/api/auth'

const TOKEN_KEY = 'weather_token'
const USER_KEY = 'weather_user'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  role: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  )

  const isLoggedIn = computed(() => !!token.value)

  async function login(username: string, password: string): Promise<LoginResult> {
    const result = await loginApi({ username, password })
    token.value = result.token
    userInfo.value = {
      id: result.id,
      username: result.username,
      nickname: result.nickname,
      role: result.role
    }
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
    return result
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, isLoggedIn, login, logout }
})
