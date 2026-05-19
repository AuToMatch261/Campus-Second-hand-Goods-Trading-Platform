import request from './request'

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
}

export interface LoginResult {
  token: string
  expireMinutes: number
  user: UserInfo
}

export const authApi = {
  register(body: RegisterRequest) {
    return request.post<unknown, UserInfo>('/auth/register', body)
  },
  login(body: LoginRequest) {
    return request.post<unknown, LoginResult>('/auth/login', body)
  },
  me() {
    return request.get<unknown, UserInfo>('/auth/me')
  },
}
