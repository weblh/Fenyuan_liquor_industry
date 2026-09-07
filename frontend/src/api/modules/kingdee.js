import request from '@/utils/request'

function createCrudApi(basePath) {
  return {
    list: (params) => request.get(basePath, { params }),
    detail: (id) => request.get(`${basePath}/${id}`),
    create: (data) => request.post(basePath, data),
    update: (id, data) => request.put(`${basePath}/${id}`, data),
    remove: (ids) => request.delete(`${basePath}/${ids}`),
  }
}

export const kingdeeCredentialApi = {
  ...createCrudApi('/kingdee/credentials'),
  listEnabled: () => request.get('/kingdee/credentials/enabled'),
  toggle: (id) => request.post(`/kingdee/credentials/${id}/toggle`),
  testLogin: (id, dbId) => request.post(`/kingdee/credentials/${id}/test-login`, {}, { params: dbId ? { dbId } : {} }),
}

export const kingdeeAccountSetApi = {
  ...createCrudApi('/kingdee/account-sets'),
  listEnabled: () => request.get('/kingdee/account-sets/enabled'),
  getDefault: () => request.get('/kingdee/account-sets/default'),
  setDefault: (id) => request.post(`/kingdee/account-sets/${id}/default`),
  toggle: (id) => request.post(`/kingdee/account-sets/${id}/toggle`),
  testLogin: (id) => request.post(`/kingdee/account-sets/${id}/test-login`),
}
