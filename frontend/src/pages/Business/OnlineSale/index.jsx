import { Alert } from 'antd'
import BizCrudPage from '@/components/BizCrudPage'
import { onlineSaleApi } from '@/api/modules/business'

export default function OnlineSalePage() {
  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="在线销售管理"
        description="统计 ERP「美福」与「即时零售」（即美团渠道，展示为美团名酒行）的销售/发货/回款。数据来自管家婆销售单同步。"
      />
      <BizCrudPage
        title="在线销售"
        permissions={{
          add: 'business:onlineSale:add',
          edit: 'business:onlineSale:edit',
          delete: 'business:onlineSale:delete',
        }}
        api={onlineSaleApi}
        fields={[
          {
            name: 'customerName',
            label: 'ERP客户',
            type: 'select',
            required: true,
            search: true,
            options: [
              { label: '美福', value: '美福' },
              { label: '美团名酒行', value: '美团名酒行' },
            ],
          },
          { name: 'periodName', label: '期间', type: 'text', search: true },
          { name: 'saleAmount', label: '销售额', type: 'number', required: true },
          { name: 'shipAmount', label: '发货额', type: 'number' },
          { name: 'paymentAmount', label: '回款额', type: 'number' },
          { name: 'remark', label: '备注', type: 'textarea', table: false },
        ]}
      />
    </>
  )
}
