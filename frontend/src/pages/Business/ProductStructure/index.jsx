import BizCrudPage from '@/components/BizCrudPage'
import { productStructureApi } from '@/api/modules/business'

export default function ProductStructurePage() {
  return (
    <BizCrudPage
      title="销售产品结构"
      permissions={{
        add: 'business:productStructure:add',
        edit: 'business:productStructure:edit',
        delete: 'business:productStructure:delete',
      }}
      api={productStructureApi}
      fields={[
        { name: 'category', label: '品类', type: 'text', required: true, search: true },
        { name: 'quantity', label: '数量', type: 'number', required: true },
        {
          name: 'ratio',
          label: '占比(%)',
          type: 'number',
          required: true,
          render: (v) => (v == null ? '-' : `${v}%`),
        },
        { name: 'customerSource', label: '客户来源', type: 'text' },
        { name: 'remark', label: '备注', type: 'textarea', table: false },
      ]}
    />
  )
}
