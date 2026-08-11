import BizCrudPage from '@/components/BizCrudPage'
import { inventoryApi } from '@/api/modules/business'

export default function InventoryPage() {
  return (
    <BizCrudPage
      title="汾源酒库存"
      permissions={{
        add: 'business:inventory:add',
        edit: 'business:inventory:edit',
        delete: 'business:inventory:delete',
      }}
      api={inventoryApi}
      fields={[
        { name: 'productName', label: '品名', type: 'text', required: true, search: true },
        { name: 'spec', label: '规格', type: 'text' },
        { name: 'quantity', label: '数量', type: 'number', required: true },
        { name: 'amount', label: '金额', type: 'number', required: true },
        { name: 'warehouse', label: '分库', type: 'text', search: true },
        { name: 'remark', label: '备注', type: 'textarea', table: false },
      ]}
    />
  )
}
