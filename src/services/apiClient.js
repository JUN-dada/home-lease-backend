const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const TOKEN_KEY = 'house_auth_token'

function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

async function parseError(response) {
  try {
    const data = await response.json()
    if (data?.message) return data.message
    return JSON.stringify(data)
  } catch (err) {
    const text = await response.text()
    return text || '请求失败'
  }
}

async function request(path, options = {}) {
  const url = `${API_BASE_URL}${path}`
  const headers = new Headers(options.headers || {})
  const body = options.body
  const token = getToken()

  if (token) {
    headers.set('X-Auth-Token', token)
  }

  if (body && !(body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(url, {
    method: options.method || 'GET',
    headers,
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    throw new Error(await parseError(response))
  }

  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }
  return response.text()
}

export const authApi = {
  async login({ username, password }) {
    return request('/api/auth/login', {
      method: 'POST',
      body: { username, password },
    })
  },
  async logout() {
    return request('/api/auth/logout', {
      method: 'POST',
    })
  },
}

export const userApi = {
  async getProfile() {
    return request('/api/users/me')
  },
  async updateProfile(payload) {
    return request('/api/users/me', {
      method: 'PUT',
      body: payload,
    })
  },
}

export const houseApi = {
  async listMine(params = { page: 0, size: 10 }) {
    const query = new URLSearchParams({
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    }).toString()
    return request(`/api/houses/mine?${query}`)
  },
  async create(payload) {
    return request('/api/houses', {
      method: 'POST',
      body: payload,
    })
  },
  async update(houseId, payload) {
    return request(`/api/houses/${houseId}`, {
      method: 'PUT',
      body: payload,
    })
  },
}

export const locationApi = {
  async listRegions() {
    return request('/api/locations/regions')
  },
  async listSubways() {
    return request('/api/locations/subways')
  },
}

export const mediaApi = {
  async upload(file, type) {
    const formData = new FormData()
    formData.append('file', file)
    const query = type ? `?type=${encodeURIComponent(type)}` : ''
    return request(`/api/media/upload${query}`, {
      method: 'POST',
      body: formData,
    })
  },
}

export { API_BASE_URL, getToken, setToken, TOKEN_KEY }
