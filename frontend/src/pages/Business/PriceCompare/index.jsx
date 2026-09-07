import { useCallback, useRef, useState } from 'react'
import { Alert, Button, Tag, message } from 'antd'
import { CloudDownloadOutlined } from '@ant-design/icons'
import BizCrudPage from '@/components/BizCrudPage'
import { priceCompareApi } from '@/api/modules/business'

function money(v) {
  return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function diffTag(sale, platform) {
  const a = Number(sale || 0)
  const b = Number(platform || 0)
  if (!b) return <Tag>-</Tag>
  const diff = a - b
  if (diff > 0) return <Tag color="error">高于平台 {money(diff)}</Tag>
  if (diff < 0) return <Tag color="success">低于平台 {money(Math.abs(diff))}</Tag>
  return <Tag>持平</Tag>
}

export default function PriceComparePage() {
  const [crawling, setCrawling] = useState(false)
  const reloadRef = useRef(null)

  const handleReady = useCallback(({ reload }) => {
    reloadRef.current = reload
  }, [])

  const onCrawl = async () => {
    setCrawling(true)
    try {
      const report = await priceCompareApi.crawlOnline()
      const ok = report && typeof report === 'object'
        ? Object.values(report).filter((v) => String(v).includes('京东=') || String(v).includes('天猫=')).length
        : 0
      if (ok > 0) {
        message.success(`已更新 ${ok} 个产品的线上酒价`)
      } else {
        message.warning('线上价拉取完成，部分站点可能防爬导致暂无结果，请稍后重试')
      }
      if (typeof reloadRef.current === 'function') {
        reloadRef.current()
      }
    } catch {
      // handled by request
    } finally {
      setCrawling(false)
    }
  }

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="酒类价格对比"
        description="点击「拉取线上酒价」从京东/天猫官方店抓取实时价（汾酒按元/件=瓶价×6；飞天茅台仅网络价按元/瓶）。汾源销售价：53青花20=2350、42青花20=2300、53巴拿马20=1820、53老白汾10=810。"
      />
      <BizCrudPage
        title="酒类价格对比"
        permissions={{
          add: 'business:priceCompare:add',
          edit: 'business:priceCompare:edit',
          delete: 'business:priceCompare:delete',
        }}
        api={priceCompareApi}
        onReady={handleReady}
        toolbarExtra={
          <Button type="primary" ghost icon={<CloudDownloadOutlined />} loading={crawling} onClick={onCrawl}>
            拉取线上酒价
          </Button>
        }
        fields={[
          { name: 'productName', label: '产品名称', type: 'text', required: true, search: true },
          { name: 'spec', label: '规格', type: 'text', render: (v) => v || '500ml' },
          { name: 'salePrice', label: '汾源销售价(元/件)', type: 'number', required: true, render: (v) => money(v) },
          { name: 'jdPrice', label: '京东官方店', type: 'number', render: (v) => (v == null ? '-' : money(v)) },
          { name: 'tmallPrice', label: '天猫官方店', type: 'number', render: (v) => (v == null ? '-' : money(v)) },
          {
            name: 'jdDiff',
            label: '较京东',
            type: 'text',
            form: false,
            render: (_, row) => diffTag(row.salePrice, row.jdPrice),
          },
          {
            name: 'tmallDiff',
            label: '较天猫',
            type: 'text',
            form: false,
            render: (_, row) => diffTag(row.salePrice, row.tmallPrice),
          },
          { name: 'crawlTime', label: '抓取时间', type: 'text' },
          { name: 'remark', label: '备注', type: 'textarea', table: false },
        ]}
      />
    </>
  )
}
