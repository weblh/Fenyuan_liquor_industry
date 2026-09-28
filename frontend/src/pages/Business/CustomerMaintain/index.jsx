import { Alert, Tag } from 'antd'
import BizCrudPage from '@/components/BizCrudPage'
import { customerMaintainApi } from '@/api/modules/business'

const ALERT_DAYS = 60

export default function CustomerMaintainPage() {
  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="客户维护监管"
        description="客户数据来自管家婆 ERP 同步。未复购天数根据 ERP 最近购买日计算，无购买记录时显示为「-」而非 0 天。"
      />
      <BizCrudPage
        title="客户维护监管"
      permissions={{
        add: 'business:customerMaintain:add',
        edit: 'business:customerMaintain:edit',
        delete: 'business:customerMaintain:delete',
      }}
      api={customerMaintainApi}
      fields={[
        { name: 'customerName', label: '客户名称', type: 'text', required: true, search: true },
        { name: 'contactPhone', label: '联系电话', type: 'text' },
        { name: 'lastPurchaseDate', label: '最近购买日', type: 'text' },
        {
          name: 'daysSincePurchase',
          label: '未复购天数',
          type: 'number',
          render: (v) => {
            if (v == null || v === '') return <Tag>-</Tag>
            const days = Number(v)
            const alert = days >= ALERT_DAYS
            return <Tag color={alert ? 'error' : 'success'}>{days} 天{alert ? ' · 需跟进' : ''}</Tag>
          },
        },
        {
          name: 'alertStatus',
          label: '预警状态',
          type: 'select',
          options: [
            { label: '正常', value: 0 },
            { label: '60天未复购', value: 1 },
          ],
          render: (v) => (Number(v) === 1 ? <Tag color="error">60天未复购</Tag> : <Tag color="success">正常</Tag>),
        },
        { name: 'remark', label: '备注', type: 'textarea', ellipsis: true },
      ]}
    />
    </>
  )
}
