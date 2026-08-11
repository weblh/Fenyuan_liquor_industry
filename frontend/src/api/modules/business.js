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

export const onlineSaleApi = createCrudApi('/business/online-sales')
export const salesRankApi = createCrudApi('/business/sales-ranks')
export const inventoryApi = createCrudApi('/business/inventories')
export const productStructureApi = createCrudApi('/business/product-structures')
export const customerDevApi = createCrudApi('/business/customer-devs')
export const receivableApi = createCrudApi('/finance/receivables')
