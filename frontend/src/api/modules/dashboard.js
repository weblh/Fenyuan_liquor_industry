import request from '@/utils/request'

/** 首页经营大屏总览 */
export function getDashboardOverview() {
  return request.get('/dashboard/overview')
}
