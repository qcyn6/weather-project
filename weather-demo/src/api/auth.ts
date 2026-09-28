import { get, post } from '@/utils/request'

export interface LoginResult {
  token: string
  id: number
  username: string
  nickname: string
  role: string
}

export function loginApi(data: { username: string; password: string }): Promise<LoginResult> {
  return post<LoginResult>('/auth/login', data)
}

export function registerApi(data: {
  username: string
  password: string
  nickname?: string
}): Promise<number> {
  return post<number>('/auth/register', data)
}

export function meApi(): Promise<LoginResult> {
  return get<LoginResult>('/auth/me')
}
