import { Alert } from 'antd'
import BizCrudPage from '@/components/BizCrudPage'
import { inventoryApi } from '@/api/modules/business'

export default function InventoryPage() {
  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="汾源酒库存"
        description="对应管家婆「存货库存详情」：可分仓库（后库+门店前厅汇总），或勾选「按存货汇总」导出全仓按品名合计。导入入口：业务管理 → 管家婆同步。"
      />
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
          {
            name: 'warehouse',
            label: '分库',
            type: 'select',
            search: true,
            options: [
              { label: '全部仓库汇总', value: '全部仓库汇总' },
              { label: '门店前厅+后库', value: '门店前厅+后库' },
              { label: '门店前厅', value: '门店前厅' },
              { label: '后库', value: '后库' },
            ],
          },
          { name: 'remark', label: '备注', type: 'textarea', table: false },
        ]}
      />
    </>
  )
}
