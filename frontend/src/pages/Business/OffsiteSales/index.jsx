import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Empty, Spin, Tag, message } from 'antd'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import BizCrudPage from '@/components/BizCrudPage'
import { offsiteSaleApi } from '@/api/modules/business'
import styles from './index.module.css'

const SOURCE_MAP = {
  ERP: 'ERP',
  JD: '京东物流',
}

let chinaMapReady = null

function ensureChinaMap() {
  if (chinaMapReady) return chinaMapReady
  chinaMapReady = fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    .then((r) => {
      if (!r.ok) throw new Error('地图加载失败')
      return r.json()
    })
    .then((geo) => {
      echarts.registerMap('china', geo)
      return true
    })
    .catch((err) => {
      chinaMapReady = null
      throw err
    })
  return chinaMapReady
}

export default function OffsiteSalesPage() {
  const [mapLoading, setMapLoading] = useState(true)
  const [mapReady, setMapReady] = useState(false)
  const [rows, setRows] = useState([])

  const loadMapData = useCallback(async () => {
    setMapLoading(true)
    try {
      await ensureChinaMap()
      setMapReady(true)
      const res = await offsiteSaleApi.list({ current: 1, size: 500 })
      const list = res?.records || res?.list || (Array.isArray(res) ? res : [])
      setRows(list)
    } catch {
      setMapReady(false)
      message.warning('异地销售地图暂不可用，仍可维护明细数据')
    } finally {
      setMapLoading(false)
    }
  }, [])

  useEffect(() => {
    loadMapData()
  }, [loadMapData])

  const provinceAgg = useMemo(() => {
    const map = {}
    rows.forEach((r) => {
      const province = (r.province || '').replace(/省|市|自治区|壮族|回族|维吾尔/g, '') || '未知'
      map[province] = (map[province] || 0) + Number(r.quantity || 0)
    })
    return Object.entries(map).map(([name, value]) => ({ name, value }))
  }, [rows])

  const mapOption = useMemo(
    () => ({
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(13, 26, 48, 0.96)',
        borderColor: 'rgba(90, 165, 255, 0.35)',
        borderWidth: 1,
        textStyle: { color: '#e8f0fc', fontSize: 13 },
        extraCssText: 'box-shadow: none; border-radius: 6px;',
        formatter: (p) => `${p.name}<br/>异地销量：${p.value || 0}`,
      },
      visualMap: {
        min: 0,
        max: Math.max(10, ...provinceAgg.map((i) => i.value), 0),
        left: 16,
        bottom: 16,
        text: ['高', '低'],
        textStyle: { color: '#9db2d0', fontSize: 12 },
        inRange: { color: ['#0e2a4d', '#2f80ed', '#3dd6f2'] },
        calculable: true,
      },
      series: [
        {
          type: 'map',
          map: 'china',
          roam: true,
          label: { show: false },
          itemStyle: {
            areaColor: '#162849',
            borderColor: 'rgba(90, 165, 255, 0.35)',
            borderWidth: 0.7,
          },
          emphasis: {
            label: { show: true, color: '#ffffff', fontWeight: 600 },
            itemStyle: { areaColor: '#3dd6f2' },
          },
          data: provinceAgg,
        },
      ],
    }),
    [provinceAgg],
  )

  return (
    <div className={styles.page}>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="异地销售统计"
        description="统计产品：42度青花20、53度青花20、53度老白汾10、53度巴拿马20；收货地址为广州行政区域范围以外的销量。"
      />

      <section className={styles.mapPanel}>
        <header className={styles.mapHead}>
          <h3>异地销售地图</h3>
          <span>按收货省份汇总销量</span>
        </header>
        <Spin spinning={mapLoading}>
          {mapReady && provinceAgg.length ? (
            <ReactECharts option={mapOption} style={{ height: 420 }} opts={{ renderer: 'canvas' }} />
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={mapReady ? '暂无异地销售数据' : '地图资源加载失败'}
              style={{ padding: '64px 0' }}
            />
          )}
        </Spin>
      </section>

      <BizCrudPage
        title="异地销售"
        permissions={{
          add: 'business:offsiteSale:add',
          edit: 'business:offsiteSale:edit',
          delete: 'business:offsiteSale:delete',
        }}
        api={{
          ...offsiteSaleApi,
          create: async (data) => {
            await offsiteSaleApi.create(data)
            loadMapData()
          },
          update: async (id, data) => {
            await offsiteSaleApi.update(id, data)
            loadMapData()
          },
          remove: async (ids) => {
            await offsiteSaleApi.remove(ids)
            loadMapData()
          },
        }}
        fields={[
          { name: 'productName', label: '产品名称', type: 'text', required: true, search: true },
          { name: 'quantity', label: '销量', type: 'number', required: true },
          { name: 'province', label: '省份', type: 'text', required: true, search: true },
          { name: 'city', label: '城市', type: 'text' },
          { name: 'address', label: '详细地址', type: 'text', ellipsis: true },
          {
            name: 'sourceType',
            label: '来源',
            type: 'select',
            options: [
              { label: 'ERP', value: 'ERP' },
              { label: '京东物流', value: 'JD' },
            ],
            render: (v) => <Tag color={v === 'JD' ? 'processing' : 'default'}>{SOURCE_MAP[v] || v || '-'}</Tag>,
          },
          { name: 'orderNo', label: '单据号', type: 'text' },
          { name: 'remark', label: '备注', type: 'textarea', table: false },
        ]}
      />
    </div>
  )
}
