import httpClient, { API_BASE_URL, getToken, setToken, TOKEN_KEY } from './httpClient'

export const authApi = {
  login({ username, password }) {
    return httpClient.post('/api/auth/login', { username, password })
  },
  register(payload) {
    return httpClient.post('/api/auth/register', payload)
  },
  logout() {
    return httpClient.post('/api/auth/logout')
  },
}

export const userApi = {
  getProfile() {
    return httpClient.get('/api/users/me')
  },
  updateProfile(payload) {
    return httpClient.put('/api/users/me', payload)
  },
  getById(userId) {
    return httpClient.get(`/api/users/${userId}`)
  },
}

export const houseApi = {
  search(params = {}) {
    return httpClient.get('/api/houses/search', {
      params,
    })
  },
  latest() {
    return httpClient.get('/api/houses/latest')
  },
  recommended() {
    return httpClient.get('/api/houses/recommended')
  },
  random(size = 4) {
    return httpClient.get('/api/houses/random', {
      params: { size },
    })
  },
  detail(houseId) {
    return httpClient.get(`/api/houses/${houseId}`)
  },
  listMine(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/houses/mine', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 10,
      },
    })
  },
  create(payload) {
    return httpClient.post('/api/houses', payload)
  },
  update(houseId, payload) {
    return httpClient.put(`/api/houses/${houseId}`, payload)
  },
  remove(houseId) {
    return httpClient.delete(`/api/houses/${houseId}`)
  },
  toggleFavorite(houseId) {
    return httpClient.post(`/api/houses/${houseId}/favorite`)
  },
  favorites() {
    return httpClient.get('/api/houses/favorites')
  },
  favoritesAll() {
    return httpClient.get('/api/houses/favorites/all')
  },
  recommend(houseId, value = true) {
    return httpClient.post(`/api/houses/${houseId}/recommend`, null, {
      params: { value },
    })
  },
  updateStatus(houseId, status) {
    return httpClient.post(`/api/houses/${houseId}/status`, null, {
      params: { status },
    })
  },
}

export const locationApi = {
  listRegions() {
    return httpClient.get('/api/locations/regions')
  },
  listSubways() {
    return httpClient.get('/api/locations/subways')
  },
  createRegion(payload) {
    return httpClient.post('/api/locations/regions', payload)
  },
  updateRegion(regionId, payload) {
    return httpClient.put(`/api/locations/regions/${regionId}`, payload)
  },
  deleteRegion(regionId) {
    return httpClient.delete(`/api/locations/regions/${regionId}`)
  },
  createSubway(payload) {
    return httpClient.post('/api/locations/subways', payload)
  },
  updateSubway(subwayId, payload) {
    return httpClient.put(`/api/locations/subways/${subwayId}`, payload)
  },
  deleteSubway(subwayId) {
    return httpClient.delete(`/api/locations/subways/${subwayId}`)
  },
}

export const mediaApi = {
  upload(file, type) {
    const formData = new FormData()
    formData.append('file', file)
    return httpClient.post('/api/media/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      params: type ? { type } : undefined,
    })
  },
}

export const contactApi = {
  create(payload) {
    return httpClient.post('/api/contacts', payload)
  },
  ensure(payload) {
    return httpClient.post('/api/contacts/ensure', payload)
  },
  mine(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/contacts/mine', { params })
  },
  landlord(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/contacts/landlord', { params })
  },
  admin(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/contacts', { params })
  },
  updateStatus(recordId, payload) {
    return httpClient.post(`/api/contacts/${recordId}/status`, payload)
  },
}

export const orderApi = {
  create(payload) {
    return httpClient.post('/api/orders', payload)
  },
  mine(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/orders/mine', { params })
  },
  landlord(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/orders/landlord', { params })
  },
  admin(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/orders', { params })
  },
  cancel(orderId) {
    return httpClient.post(`/api/orders/${orderId}/cancel`)
  },
  confirm(orderId) {
    return httpClient.post(`/api/orders/${orderId}/confirm`)
  },
  activate(orderId) {
    return httpClient.post(`/api/orders/${orderId}/activate`)
  },
  uploadContract(orderId, payload) {
    return httpClient.post(`/api/orders/${orderId}/contract`, payload)
  },
  downloadContract(orderId) {
    return httpClient.get(`/api/orders/${orderId}/contract`)
  },
  terminate(orderId, payload) {
    return httpClient.post(`/api/orders/${orderId}/terminate`, payload)
  },
  approveTermination(orderId, payload) {
    return httpClient.post(`/api/orders/${orderId}/termination/approve`, payload)
  },
  rejectTermination(orderId, payload) {
    return httpClient.post(`/api/orders/${orderId}/termination/reject`, payload)
  },
}

export const announcementApi = {
  list(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/announcements', { params })
  },
  latest() {
    return httpClient.get('/api/announcements/latest')
  },
  create(payload) {
    return httpClient.post('/api/announcements', payload)
  },
  update(id, payload) {
    return httpClient.put(`/api/announcements/${id}`, payload)
  },
  remove(id) {
    return httpClient.delete(`/api/announcements/${id}`)
  },
}

export const certificationApi = {
  submit(payload) {
    return httpClient.post('/api/certifications', payload)
  },
  me() {
    return httpClient.get('/api/certifications/me')
  },
  list(params = { page: 0, size: 10, status: 'PENDING' }) {
    return httpClient.get('/api/certifications', { params })
  },
  review(certificationId, payload) {
    return httpClient.post(`/api/certifications/${certificationId}/review`, payload)
  },
}

export const statisticsApi = {
  summary() {
    return httpClient.get('/api/statistics')
  },
}

export const adminUserApi = {
  listByRole(role) {
    return httpClient.get('/api/users', {
      params: { role },
    })
  },
  create(payload) {
    return httpClient.post('/api/users', payload)
  },
  remove(userId) {
    return httpClient.delete(`/api/users/${userId}`)
  },
}

export const supportApi = {
  create(payload) {
    return httpClient.post('/api/support/tickets', payload)
  },
  list(params = { page: 0, size: 10 }) {
    return httpClient.get('/api/support/tickets', { params })
  },
  adminList(params = { page: 0, size: 10, status: null }) {
    return httpClient.get('/api/support/admin/tickets', { params })
  },
  messages(ticketId, params = { page: 0, size: 50 }) {
    return httpClient.get(`/api/support/tickets/${ticketId}/messages`, { params })
  },
}

export { API_BASE_URL, getToken, setToken, TOKEN_KEY }
