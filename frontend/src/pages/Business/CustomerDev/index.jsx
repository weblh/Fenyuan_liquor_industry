import { Alert, Tag } from 'antd'
import BizCrudPage from '@/components/BizCrudPage'
import { customerDevApi } from '@/api/modules/business'

function formatMonth(v) {
  if (!v) return '-'
  const [y, m] = String(v).split('-')
  if (!m) return v
  return `${y}年${Number(m)}月`
}

export default function CustomerDevPage() {
  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="客户开发"
        description="展示当月新开销售客户：管家婆历史中此前从未销售过，但当月首次产生销售的客户。"
      />
      <BizCrudPage
        title="客户开发"
        permissions={{
          add: 'business:customerDev:add',
          edit: 'business:customerDev:edit',
          delete: 'business:customerDev:delete',
        }}
        api={customerDevApi}
        fields={[
          { name: 'name', label: '客户名称', type: 'text', required: true, search: true },
          {
            name: 'openMonth',
            label: '新开月份',
            type: 'text',
            required: true,
            render: (v) => <Tag color="processing">{formatMonth(v)}</Tag>,
          },
          { name: 'amount', label: '当月销售额', type: 'number' },
          {
            name: 'status',
            label: '状态',
            type: 'select',
            options: [{ label: '开发成功', value: 1 }],
            form: false,
            render: () => <Tag color="success">开发成功</Tag>,
          },
          { name: 'remark', label: '备注', type: 'textarea', ellipsis: true },
        ]}
      />
    </>
  )
}
