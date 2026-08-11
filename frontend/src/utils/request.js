import axios from 'axios'
import { message } from 'antd'
import { getToken, removeToken } from './storage'

const apiBase = `${import.meta.env.BASE_URL}api`
const loginPath = `${import.meta.env.BASE_URL}login`

const request = axios.create({
  baseURL: apiBase,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }
    const res = response.data
    if (res == null || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    const { code, msg, message: msg2, data } = res
    if (code === 200) {
      return data
    }
    if (code === 401) {
      removeToken()
      message.error(msg || msg2 || '登录已过期，请重新登录')
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = `${loginPath}?redirect=${encodeURIComponent(window.location.pathname)}`
      }
      return Promise.reject(new Error(msg || '未授权'))
    }
    message.error(msg || msg2 || '请求失败')
    return Promise.reject(new Error(msg || msg2 || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = loginPath
      }
    }
    const msg = error.response?.data?.msg || error.response?.data?.message || error.message || '网络错误'
    message.error(msg)
    return Promise.reject(error)
  }
)

export default request
