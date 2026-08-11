import request from '@/utils/request'

export function getConfigList(params) {
  return request.get('/configs', { params })
}

export function getAllConfigs() {
  return request.get('/configs/all')
}

export function updateConfig(id, data) {
  return request.put(`/configs/${id}`, data)
}

export function updateConfigBatch(data) {
  return request.put('/configs', data)
}
