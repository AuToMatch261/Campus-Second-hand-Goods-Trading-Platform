import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi, type LoginRequest, type RegisterRequest, type UserInfo } from '@/api/auth'
import { userApi, type Profile, type UpdateProfileRequest } from '@/api/user'

export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string>('')
    const profile = ref<Profile | null>(null)

    async function login(req: LoginRequest) {
      const r = await authApi.login(req)
      token.value = r.token
      profile.value = { ...r.user }
    }

    async function register(req: RegisterRequest) {
      await authApi.register(req)
    }

    async function refreshProfile() {
      if (!token.value) return null
      profile.value = await userApi.myProfile()
      return profile.value
    }

    async function updateProfile(req: UpdateProfileRequest) {
      profile.value = await userApi.updateMyProfile(req)
      return profile.value
    }

    function logout() {
      token.value = ''
      profile.value = null
    }

    return { token, profile, login, register, refreshProfile, updateProfile, logout }
  },
  {
    persist: {
      key: 'campus-user',
      paths: ['token', 'profile'],
    },
  },
)

export type { Profile, UpdateProfileRequest }
export type { UserInfo, LoginRequest, RegisterRequest }
