import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
export const TOKEN_KEY = 'house_auth_token'

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token) {
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  } catch {
    // ignore storage errors
  }
}

const httpClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})

httpClient.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['X-Auth-Token'] = token
    }
    return config
  },
  (error) => Promise.reject(error),
)

httpClient.interceptors.response.use(
  (response) => {
    if (response.status === 204) {
      return null
    }
    return response.data
  },
  (error) => {
    if (error.response) {
      const { data } = error.response
      if (data && typeof data === 'object' && data.message) {
        return Promise.reject(new Error(data.message))
      }
      if (typeof data === 'string') {
        return Promise.reject(new Error(data))
      }
      if (data) {
        return Promise.reject(new Error(JSON.stringify(data)))
      }
    }
    return Promise.reject(new Error(error.message || '请求失败'))
  },
)

export default httpClient
