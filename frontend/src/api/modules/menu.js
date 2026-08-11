import request from '@/utils/request'

export function getMenuList(params) {
  return request.get('/menus', { params })
}

export function getMenuDetail(id) {
  return request.get(`/menus/${id}`)
}

export function addMenu(data) {
  return request.post('/menus', data)
}

export function updateMenu(id, data) {
  return request.put(`/menus/${id}`, data)
}

export function deleteMenus(ids) {
  return request.delete(`/menus/${ids}`)
}

export function getUserMenus() {
  return request.get('/menus/user')
}

export function getMenuRoutes() {
  return request.get('/menus/routes')
}
