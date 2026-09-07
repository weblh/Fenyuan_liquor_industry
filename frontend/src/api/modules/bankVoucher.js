import request from '@/utils/request'

export function searchBankVouchers(params) {
  return request.get('/kingdee/bank-voucher/search', { params })
}

export function listBankChannels() {
  return request.get('/kingdee/bank-voucher/channels')
}

export function importBankVouchers(file, channelKey) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/kingdee/bank-voucher/import', form, {
    params: channelKey ? { channelKey } : undefined,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function clearAllBankVouchers() {
  return request.delete('/kingdee/bank-voucher/clear-all')
}

export function inferBankVoucherTypes(data) {
  return request.post('/kingdee/bank-voucher/infer-types', data, { timeout: 180000 })
}

export function checkBankKingdeeExists(data) {
  return request.post('/kingdee/bank-voucher/check-kingdee-exists', data, { timeout: 180000 })
}

export function writeBankVouchers(data) {
  return request.post('/kingdee/bank-voucher/write', data, { timeout: 300000 })
}
