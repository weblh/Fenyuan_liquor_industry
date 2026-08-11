import request from '@/utils/request'

export function getRoleList(params) {
  return request.get('/roles', { params })
}

export function getAllRoles() {
  return request.get('/roles/all')
}

export function getRoleDetail(id) {
  return request.get(`/roles/${id}`)
}

export function addRole(data) {
  return request.post('/roles', data)
}

export function updateRole(id, data) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRoles(ids) {
  return request.delete(`/roles/${ids}`)
}

export function updateRoleStatus(id, status) {
  return request.put(`/roles/${id}/status`, { status })
}

export function getRolePermissions(id) {
  return request.get(`/roles/${id}/permissions`)
}

export function assignRolePermissions(id, menuIds) {
  return request.put(`/roles/${id}/permissions`, { menuIds })
}
