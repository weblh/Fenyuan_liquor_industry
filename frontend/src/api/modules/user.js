import request from '@/utils/request'

export function getUserList(params) {
  return request.get('/users', { params })
}

export function getUserDetail(id) {
  return request.get(`/users/${id}`)
}

export function addUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function deleteUsers(ids) {
  return request.delete(`/users/${ids}`)
}

export function updateUserStatus(id, status) {
  return request.put(`/users/${id}/status`, { status })
}

export function resetUserPassword(id, data) {
  return request.put(`/users/${id}/password`, data)
}
