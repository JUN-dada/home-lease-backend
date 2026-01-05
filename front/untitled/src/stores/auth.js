import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi, userApi, getToken, setToken } from '../services/apiClient'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => Boolean(token.value))
  const userName = computed(() => user.value?.fullName || user.value?.username || '')
  const userRole = computed(() => user.value?.role || '')

  function resetError() {
    error.value = null
  }

  async function login(username, password) {
    loading.value = true
    resetError()
    try {
      const response = await authApi.login({ username, password })
      token.value = response.token
      setToken(response.token)
      user.value = response
      return response
    } catch (err) {
      error.value = err.message || '登录失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function register(payload) {
    loading.value = true
    resetError()
    try {
      const response = await authApi.register(payload)
      // 注册成功后直接登录
      token.value = response.token
      setToken(response.token)
      user.value = response
      return response
    } catch (err) {
      error.value = err.message || '注册失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchProfile() {
    if (!token.value) return null
    loading.value = true
    resetError()
    try {
      const profile = await userApi.getProfile()
      user.value = { ...(user.value || {}), ...profile }
      return profile
    } catch (err) {
      error.value = err.message || '获取用户信息失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(payload) {
    loading.value = true
    resetError()
    try {
      const updated = await userApi.updateProfile(payload)
      user.value = { ...(user.value || {}), ...updated }
      return updated
    } catch (err) {
      error.value = err.message || '更新资料失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // ignore backend failures
    } finally {
      token.value = null
      user.value = null
      setToken(null)
    }
  }

  async function bootstrap() {
    if (!token.value || user.value) return
    try {
      await fetchProfile()
    } catch {
      await logout()
    }
  }

  function hasRole(roles) {
    const currentRole = userRole.value
    if (!roles || roles.length === 0) return true
    return roles.includes(currentRole)
  }

  return {
    token,
    user,
    loading,
    error,
    isAuthenticated,
    userName,
    userRole,
    login,
    register,
    fetchProfile,
    updateProfile,
    logout,
    bootstrap,
    resetError,
    hasRole,
  }
})
