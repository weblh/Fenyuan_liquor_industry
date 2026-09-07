import { Alert } from 'antd'
import BizCrudPage from '@/components/BizCrudPage'
import { productStructureApi } from '@/api/modules/business'

export default function ProductStructurePage() {
  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="销售产品结构"
        description="按系列分组展示，如：汾酒系列（42度巴拿马20、53度青花20…）、茅台系列（53度飞天茅台、茅台1935…），数量单位为箱。"
      />
      <BizCrudPage
        title="销售产品结构"
        permissions={{
          add: 'business:productStructure:add',
          edit: 'business:productStructure:edit',
          delete: 'business:productStructure:delete',
        }}
        api={productStructureApi}
        fields={[
          {
            name: 'series',
            label: '系列',
            type: 'select',
            required: true,
            search: true,
            options: [
              { label: '汾酒系列', value: '汾酒系列' },
              { label: '茅台系列', value: '茅台系列' },
            ],
          },
          { name: 'productName', label: '产品', type: 'text', required: true, search: true },
          {
            name: 'quantity',
            label: '销量(箱)',
            type: 'number',
            required: true,
            render: (v) => (v == null ? '-' : `${v} 箱`),
          },
          {
            name: 'ratio',
            label: '占比(%)',
            type: 'number',
            render: (v) => (v == null ? '-' : `${v}%`),
          },
          { name: 'category', label: '品类(兼容)', type: 'text', table: false, form: false },
          { name: 'customerSource', label: '客户来源', type: 'text' },
          { name: 'remark', label: '备注', type: 'textarea', table: false },
        ]}
      />
    </>
  )
}
