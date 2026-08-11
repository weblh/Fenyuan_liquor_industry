import BizCrudPage from '@/components/BizCrudPage'
import { onlineSaleApi } from '@/api/modules/business'

export default function OnlineSalePage() {
  return (
    <BizCrudPage
      title="在线销售"
      permissions={{
        add: 'business:onlineSale:add',
        edit: 'business:onlineSale:edit',
        delete: 'business:onlineSale:delete',
      }}
      api={onlineSaleApi}
      fields={[
        { name: 'periodName', label: '期间', type: 'text', search: true },
        { name: 'saleAmount', label: '销售', type: 'number', required: true },
        { name: 'shipAmount', label: '发货', type: 'number', required: true },
        { name: 'paymentAmount', label: '回款', type: 'number', required: true },
        { name: 'remark', label: '备注', type: 'textarea', table: false },
      ]}
    />
  )
}
