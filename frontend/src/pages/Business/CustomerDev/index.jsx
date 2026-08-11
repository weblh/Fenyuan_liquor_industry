import BizCrudPage from '@/components/BizCrudPage'
import { customerDevApi } from '@/api/modules/business'

export default function CustomerDevPage() {
  return (
    <BizCrudPage
      title="客户开发"
      permissions={{
        add: 'business:customerDev:add',
        edit: 'business:customerDev:edit',
        delete: 'business:customerDev:delete',
      }}
      api={customerDevApi}
      fields={[
        { name: 'name', label: '名称', type: 'text', required: true, search: true },
        { name: 'amount', label: '金额', type: 'number', required: true },
        { name: 'remark', label: '备注', type: 'textarea', ellipsis: true },
      ]}
    />
  )
}
