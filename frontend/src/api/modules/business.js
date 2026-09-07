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
export const customerMaintainApi = createCrudApi('/business/customer-maintains')
export const priceCompareApi = {
  ...createCrudApi('/business/price-compares'),
  crawlOnline: () => request.post('/business/price-compares/crawl-online', null, { timeout: 120000 }),
}
export const offsiteSaleApi = createCrudApi('/business/offsite-sales')
export const receivableApi = createCrudApi('/finance/receivables')
export const kingdeeVoucherApi = createCrudApi('/finance/kingdee-vouchers')
export const cmcloudSyncApi = {
  syncMaster: () => request.post('/integration/cmcloud/sync-master'),
  syncBills: () => request.post('/integration/cmcloud/sync-bills', null, { timeout: 300000 }),
  importSalesExcel: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/integration/cmcloud/import-sales-excel', form, {
      timeout: 300000,
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  importInventoryExcel: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/integration/cmcloud/import-inventory-excel', form, {
      timeout: 300000,
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
