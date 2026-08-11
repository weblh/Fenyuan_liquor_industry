import BizCrudPage from '@/components/BizCrudPage'
import { receivableApi } from '@/api/modules/business'

export default function ReceivablePage() {
  return (
    <BizCrudPage
      title="应收账款"
      permissions={{
        add: 'finance:receivable:add',
        edit: 'finance:receivable:edit',
        delete: 'finance:receivable:delete',
      }}
      api={receivableApi}
      fields={[
        { name: 'name', label: '名称', type: 'text', required: true, search: true },
        { name: 'amount', label: '金额', type: 'number', required: true },
        { name: 'remark', label: '备注', type: 'textarea', table: false },
      ]}
    />
  )
}
