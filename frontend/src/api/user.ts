import request from './request'

export interface Profile {
  id: number
  username: string
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
}

export interface UpdateProfileRequest {
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
}

export const userApi = {
  myProfile() {
    return request.get<unknown, Profile>('/user/profile/me')
  },
  updateMyProfile(body: UpdateProfileRequest) {
    return request.put<unknown, Profile>('/user/profile/me', body)
  },
  getById(userId: number) {
    return request.get<unknown, Profile>(`/user/profile/${userId}`)
  },
}
