import request from '@/utils/request'

/** 获取验证码 */
export function getCaptcha() {
  return request.get('/auth/captcha')
}

/** 登录 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 退出 */
export function logout() {
  return request.post('/auth/logout')
}

/** 当前用户信息（含 menus / permissions） */
export function getInfo() {
  return request.get('/auth/info')
}

/** 刷新 token */
export function refreshToken() {
  return request.post('/auth/refresh')
}
