import request from '@/utils/request'

/** 部门树 */
export function getDeptList(params) {
  return request.get('/depts', { params })
}

/** 兼容旧调用：后端树接口为 GET /depts */
export function getDeptTree(params) {
  return request.get('/depts', { params })
}

export function getDeptDetail(id) {
  return request.get(`/depts/${id}`)
}

export function addDept(data) {
  return request.post('/depts', data)
}

export function updateDept(id, data) {
  return request.put(`/depts/${id}`, data)
}

export function deleteDepts(ids) {
  return request.delete(`/depts/${ids}`)
}
