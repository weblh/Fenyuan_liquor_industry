import request from '@/utils/request'

export function getOperLogList(params) {
  return request.get('/logs/oper', { params })
}

export function getLoginLogList(params) {
  return request.get('/logs/login', { params })
}
