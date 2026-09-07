import BizCrudPage from '@/components/BizCrudPage'
import { Tag, Alert } from 'antd'
import { kingdeeVoucherApi } from '@/api/modules/business'

function money(v) {
  return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

/**
 * 财务侧凭证记录页（历史菜单）。
 * 实际「写入金蝶」在「金蝶 → 银行流水凭证」；管家婆业务同步在「业务管理 → 管家婆同步」。
 */
export default function KingdeeVoucherPage() {
  return (
    <>
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message="说明"
        description="金蝶写凭证请到「金蝶 → 银行流水凭证」。销售/库存等业务数据请到「业务管理 → 管家婆同步」（对接 CMCloud，与金蝶无关）。"
      />
      <BizCrudPage
        title="凭证记录"
        permissions={{
          add: 'finance:kingdeeVoucher:add',
          edit: 'finance:kingdeeVoucher:edit',
          delete: 'finance:kingdeeVoucher:delete',
        }}
        api={kingdeeVoucherApi}
        fields={[
          { name: 'bankFlowNo', label: '银行流水号', type: 'text', required: true, search: true },
          { name: 'counterparty', label: '对方户名', type: 'text', search: true },
          { name: 'amount', label: '金额', type: 'number', required: true, render: (v) => money(v) },
          { name: 'flowDate', label: '流水日期', type: 'text' },
          {
            name: 'syncStatus',
            label: '同步状态',
            type: 'select',
            options: [
              { label: '待同步', value: 0 },
              { label: '已写入金蝶', value: 1 },
              { label: '同步失败', value: 2 },
            ],
            render: (v) => {
              if (Number(v) === 1) return <Tag color="success">已写入金蝶</Tag>
              if (Number(v) === 2) return <Tag color="error">同步失败</Tag>
              return <Tag color="processing">待同步</Tag>
            },
          },
          { name: 'voucherNo', label: '金蝶凭证号', type: 'text' },
          { name: 'syncTime', label: '同步时间', type: 'text' },
          { name: 'remark', label: '备注', type: 'textarea', table: false },
        ]}
      />
    </>
  )
}
