import { Tag } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, MinusOutlined } from '@ant-design/icons'
import BizCrudPage from '@/components/BizCrudPage'
import { salesRankApi } from '@/api/modules/business'

const trendOptions = [
  { label: '上升', value: 1 },
  { label: '下降', value: -1 },
  { label: '持平', value: 0 },
]

export default function SalesRankPage() {
  return (
    <BizCrudPage
      title="销售排名"
      permissions={{
        add: 'business:salesRank:add',
        edit: 'business:salesRank:edit',
        delete: 'business:salesRank:delete',
      }}
      api={salesRankApi}
      fields={[
        { name: 'companyName', label: '公司', type: 'text', required: true, search: true },
        { name: 'amount', label: '金额', type: 'number', required: true },
        {
          name: 'salesRatio',
          label: '销售占比(%)',
          type: 'number',
          required: true,
          render: (v) => (v == null ? '-' : `${v}%`),
        },
        {
          name: 'trend',
          label: '升降',
          type: 'select',
          required: true,
          options: trendOptions,
          render: (v) => {
            if (v === 1 || v === '1') return <Tag color="success" icon={<ArrowUpOutlined />}>上升</Tag>
            if (v === -1 || v === '-1') return <Tag color="error" icon={<ArrowDownOutlined />}>下降</Tag>
            return <Tag icon={<MinusOutlined />}>持平</Tag>
          },
        },
        { name: 'remark', label: '备注', type: 'textarea', table: false },
      ]}
    />
  )
}
