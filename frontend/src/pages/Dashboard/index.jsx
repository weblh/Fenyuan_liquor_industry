import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Empty, Spin, Table, message } from 'antd'
import {
  CompressOutlined,
  ExpandOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import { useSelector } from 'react-redux'
import { getDashboardOverview } from '@/api'
import { priceCompareApi } from '@/api/modules/business'
import styles from './index.module.css'

const MONEY = (v) => {
  const n = Number(v || 0)
  if (n >= 10000) return `${(n / 10000).toFixed(2)}万`
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const EMPTY_DATA = {
  summary: {},
  onlineSaleTrend: [],
  salesRank: [],
  productStructure: [],
  inventory: [],
  customerDev: [],
  receivable: [],
  retentionAlerts: [],
  offsiteSales: [],
}

let chinaMapReady = null

function ensureChinaMap() {
  if (chinaMapReady) return chinaMapReady
  const sources = [
    'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json',
    'https://cdn.jsdelivr.net/npm/echarts@4.9.0/map/json/china.json',
  ]
  chinaMapReady = (async () => {
    let lastErr
    for (const url of sources) {
      try {
        const r = await fetch(url)
        if (!r.ok) throw new Error(`地图加载失败: ${r.status}`)
        const geo = await r.json()
        echarts.registerMap('china', geo)
        return true
      } catch (err) {
        lastErr = err
      }
    }
    chinaMapReady = null
    throw lastErr || new Error('地图加载失败')
  })()
  return chinaMapReady
}

function formatNow() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 中英双语标题，参考酒店/丰驰大屏标识 */
function BiTitle({ zh, en, as: Tag = 'h3', className }) {
  return (
    <Tag className={className || styles.biTitle}>
      <span className={styles.biZh}>{zh}</span>
      {en ? <span className={styles.biEn}>{en}</span> : null}
    </Tag>
  )
}

export default function Dashboard() {
  const navigate = useNavigate()
  const title = useSelector((s) => s.app.title)
  const userInfo = useSelector((s) => s.user.userInfo)
  const name = userInfo?.nickname || userInfo?.username || '管理员'

  const [loading, setLoading] = useState(true)
  const [data, setData] = useState(EMPTY_DATA)
  const [updatedAt, setUpdatedAt] = useState('')
  const [fullscreen, setFullscreen] = useState(false)
  const [chartKey, setChartKey] = useState(0)
  const [mapReady, setMapReady] = useState(false)
  const [priceRows, setPriceRows] = useState([])
  const pageRef = useRef(null)
  const mapChartRef = useRef(null)

  // 地图无数据省份悬停时强制隐藏飘窗
  const onMapEvents = useMemo(() => ({
    mouseover: (params) => {
      if (params?.seriesType === 'map' && (!params.value || Number(params.value) <= 0)) {
        const inst = mapChartRef.current?.getEchartsInstance?.()
        setTimeout(() => inst?.dispatchAction({ type: 'hideTip' }), 0)
      }
    },
  }), [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [res, priceRes] = await Promise.all([
        getDashboardOverview(),
        priceCompareApi.list().catch(() => []),
      ])
      setData(res || EMPTY_DATA)
      setPriceRows(Array.isArray(priceRes) ? priceRes : (priceRes?.records || []))
      setUpdatedAt(formatNow())
    } catch {
      setData(EMPTY_DATA)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    ensureChinaMap()
      .then(() => setMapReady(true))
      .catch(() => setMapReady(false))
  }, [])

  useEffect(() => {
    const onFsChange = () => {
      const el = pageRef.current
      const active = !!(
        document.fullscreenElement === el ||
        document.webkitFullscreenElement === el
      )
      setFullscreen(active)
      // 全屏切换后重绘图表，适配新尺寸
      setChartKey((k) => k + 1)
    }
    document.addEventListener('fullscreenchange', onFsChange)
    document.addEventListener('webkitfullscreenchange', onFsChange)
    return () => {
      document.removeEventListener('fullscreenchange', onFsChange)
      document.removeEventListener('webkitfullscreenchange', onFsChange)
    }
  }, [])

  const toggleFullscreen = async () => {
    const el = pageRef.current
    if (!el) return
    try {
      const active =
        document.fullscreenElement === el || document.webkitFullscreenElement === el
      if (active) {
        if (document.exitFullscreen) await document.exitFullscreen()
        else if (document.webkitExitFullscreen) document.webkitExitFullscreen()
      } else if (el.requestFullscreen) {
        await el.requestFullscreen()
      } else if (el.webkitRequestFullscreen) {
        el.webkitRequestFullscreen()
      } else {
        message.warning('当前浏览器不支持全屏')
      }
    } catch {
      message.error('全屏切换失败')
    }
  }

  const trendOption = useMemo(() => {
    const list = data.onlineSaleTrend || []
    const periods = [...new Set(list.map((i) => i.periodName || '-'))].sort()
    const channels = ['美福', '美团名酒行']
    const byKey = {}
    list.forEach((i) => {
      const channel = i.customerName || '美福'
      byKey[`${channel}|${i.periodName || '-'}`] = Number(i.saleAmount || 0)
    })
    const monthLabel = (p) => {
      const m = String(p).match(/-(\d{2})$/)
      return m ? `${Number(m[1])}月` : p
    }
    return {
      color: ['#c9a227', '#6a9fd8'],
      tooltip: { trigger: 'axis', appendToBody: true, confine: true, textStyle: { fontSize: 11, color: '#e0e6ed', lineHeight: 13 }, padding: 0, borderWidth: 0, backgroundColor: 'transparent', borderColor: 'transparent', shadowBlur: 0, shadowColor: 'transparent', extraCssText: 'white-space:nowrap;background:transparent;border:none;box-shadow:none !important;' },
      legend: {
        data: channels.map((c) => `${c}销售额`),
        top: 0,
        right: 0,
        itemWidth: 16,
        itemHeight: 8,
        itemGap: 14,
        textStyle: { color: '#8c9bb0', fontSize: 12, fontWeight: 500 },
        inactiveColor: 'rgba(140, 155, 176, 0.35)',
      },
      grid: { left: 50, right: 14, top: 34, bottom: 26 },
      xAxis: {
        type: 'category',
        data: periods.map(monthLabel),
        axisLine: { lineStyle: { color: 'rgba(120, 170, 230, 0.18)' } },
        axisTick: { show: false },
        axisLabel: { color: '#7a8ba0', fontSize: 12, fontWeight: 500 },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: '#6a7a8a',
          fontSize: 11,
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        splitLine: { lineStyle: { type: 'dashed', color: 'rgba(120, 170, 230, 0.08)' } },
      },
      series: channels.map((channel, idx) => {
        const isMain = idx === 0
        const baseColor = isMain ? '#c9a227' : '#6a9fd8'
        return {
          name: `${channel}销售额`,
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 7,
          lineStyle: {
            width: 2.5,
            color: baseColor,
            shadowColor: isMain ? 'rgba(201, 162, 39, 0.35)' : 'rgba(106, 159, 216, 0.30)',
            shadowBlur: 8,
          },
          itemStyle: {
            color: baseColor,
            borderColor: '#0a1929',
            borderWidth: 1.5,
          },
          areaStyle: isMain
            ? {
                color: {
                  type: 'linear',
                  x: 0, y: 0, x2: 0, y2: 1,
                  colorStops: [
                    { offset: 0, color: 'rgba(201, 162, 39, 0.22)' },
                    { offset: 1, color: 'rgba(201, 162, 39, 0.02)' },
                  ],
                },
              }
            : undefined,
          data: periods.map((p) => byKey[`${channel}|${p}`] || 0),
          label: {
            show: true,
            position: 'top',
            color: baseColor,
            fontSize: 11,
            fontWeight: 600,
            formatter: (p) => (p.value ? MONEY(p.value) : ''),
          },
          emphasis: {
            scale: true,
            focus: 'series',
            lineStyle: { width: 3.2 },
            itemStyle: {
              borderColor: '#fff',
              borderWidth: 2,
              shadowColor: baseColor,
              shadowBlur: 12,
            },
          },
        }
      }),
    }
  }, [data.onlineSaleTrend])

  const structureOption = useMemo(() => {
    const list = data.productStructure || []
    const seriesMap = {}
    list.forEach((i) => {
      const series = i.series || i.category || '其他'
      const label = i.productName ? `${series} · ${i.productName}` : series
      seriesMap[label] = (seriesMap[label] || 0) + Number(i.quantity || i.ratio || 0)
    })
    // 调色板：每个颜色附带亮色用于径向渐变填色，增强立体感
    const palette = [
      { base: '#c9a227', light: '#e8c466' },
      { base: '#6a9fd8', light: '#9bc4e8' },
      { base: '#6b9f78', light: '#9cc5a6' },
      { base: '#4a6fa5', light: '#7a9bc4' },
      { base: '#c87a4a', light: '#e0a07a' },
      { base: '#7a5c4e', light: '#a07e6e' },
    ]
    const truncate = (s, n) => (s && s.length > n ? `${s.slice(0, n)}…` : s || '')
    const entries = Object.entries(seriesMap)
    const chartData = entries.map(([name, value], idx) => {
      const c = palette[idx % palette.length]
      return {
        name,
        value,
        itemStyle: {
          color: {
            type: 'radial',
            x: 0.5,
            y: 0.4,
            r: 0.8,
            colorStops: [
              { offset: 0, color: c.light },
              { offset: 1, color: c.base },
            ],
          },
          borderColor: 'rgba(255, 250, 243, 0.9)',
          borderWidth: 1.5,
          shadowColor: 'rgba(0, 0, 0, 0.35)',
          shadowBlur: 8,
        },
        label: { color: c.base },
      }
    })
    return {
      color: palette.map((c) => c.base),
      tooltip: {
        trigger: 'item',
        appendToBody: true,
        confine: true,
        textStyle: { fontSize: 11, color: '#e0e6ed', lineHeight: 13 },
        padding: 0,
        borderWidth: 0,
        backgroundColor: 'transparent',
        shadowBlur: 0,
        extraCssText: 'white-space:nowrap;background:transparent;border:none;box-shadow:none !important;',
        formatter: (p) => `${p.name}<br/>销量 Qty：${p.value} 箱<br/>占比 Share：${p.percent}%`,
      },
      legend: { show: false },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '48%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 4,
            borderColor: 'rgba(255, 250, 243, 0.9)',
            borderWidth: 1.5,
          },
          label: {
            show: true,
            position: 'outside',
            fontSize: 12,
            fontWeight: 700,
            color: '#eef3f8',
            formatter: (p) => {
              const parts = p.name.split('·').map((s) => s.trim())
              const name = parts.length > 1 ? parts.slice(1).join('·') : parts[0]
              return truncate(name, 8)
            },
            textShadow: '0 1px 2px rgba(0,0,0,0.5)',
          },
          labelLine: {
            show: true,
            length: 10,
            length2: 12,
            smooth: true,
            lineStyle: { color: 'rgba(140, 157, 176, 0.4)', width: 1 },
          },
          labelLayout: { hideOverlap: false },
          emphasis: {
            scale: true,
            scaleSize: 8,
            label: { show: true, fontWeight: 700 },
            itemStyle: {
              shadowBlur: 14,
              shadowColor: 'rgba(201, 162, 39, 0.35)',
              borderColor: 'rgba(255, 250, 243, 1)',
              borderWidth: 2,
            },
          },
          data: chartData,
        },
      ],
    }
  }, [data.productStructure])

  const rankOption = useMemo(() => {
    const list = [...(data.salesRank || [])].slice(0, 8).reverse()
    const total = list.reduce((s, i) => s + Number(i.amount || 0), 0)
    return {
      tooltip: {
        trigger: 'axis',
        appendToBody: true,
        confine: true,
        axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(201, 162, 39, 0.06)' } },
        textStyle: { fontSize: 11, color: '#e0e6ed', lineHeight: 13 },
        padding: 0,
        borderWidth: 0,
        backgroundColor: 'transparent',
        shadowBlur: 0,
        extraCssText: 'white-space:nowrap;background:transparent;border:none;box-shadow:none !important;',
        formatter: (params) => {
          const p = params?.[0]
          if (!p) return ''
          const pct = total > 0 ? ((p.value / total) * 100).toFixed(1) : '0'
          return `${p.name}<br/>销售额：${MONEY(p.value)}（${pct}%）`
        },
      },
      grid: { left: 96, right: 58, top: 8, bottom: 8 },
      xAxis: {
        type: 'value',
        axisLabel: {
          color: '#5a6a7a',
          fontSize: 11,
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { type: 'dashed', color: 'rgba(120, 170, 230, 0.07)' } },
      },
      yAxis: {
        type: 'category',
        data: list.map((i) => i.companyName),
        axisLabel: {
          color: '#8c9bb0',
          width: 86,
          overflow: 'truncate',
          fontSize: 12,
          fontWeight: 500,
        },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          type: 'bar',
          barMaxWidth: 18,
          data: list.map((i, idx) => ({
            value: Number(i.amount || 0),
            itemStyle: {
              borderRadius: [0, 5, 5, 0],
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 1, y2: 0,
                colorStops: [
                  { offset: 0, color: idx === list.length - 1 ? 'rgba(201, 162, 39, 0.45)' : 'rgba(201, 162, 39, 0.65)' },
                  { offset: 1, color: '#e0b838' },
                ],
              },
              shadowColor: 'rgba(201, 162, 39, 0.30)',
              shadowBlur: 6,
            },
          })),
          label: {
            show: true,
            position: 'right',
            color: '#e0b838',
            fontSize: 12,
            fontWeight: 600,
            formatter: (p) => MONEY(p.value),
            textShadow: '0 0 6px rgba(224, 184, 56, 0.30)',
          },
          emphasis: {
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 1, y2: 0,
                colorStops: [
                  { offset: 0, color: 'rgba(224, 184, 56, 0.75)' },
                  { offset: 1, color: '#f0c840' },
                ],
              },
              shadowColor: 'rgba(224, 184, 56, 0.55)',
              shadowBlur: 12,
              borderColor: 'rgba(255, 250, 243, 0.9)',
              borderWidth: 1,
            },
            label: { color: '#f8d870', fontWeight: 700 },
          },
        },
      ],
    }
  }, [data.salesRank])

  // 省份短名 → ECharts 中国地图 JSON 中的标准全名
  const PROVINCE_FULL_NAMES = {
    北京: '北京市', 天津: '天津市', 上海: '上海市', 重庆: '重庆市',
    内蒙古: '内蒙古自治区', 广西: '广西壮族自治区', 西藏: '西藏自治区',
    宁夏: '宁夏回族自治区', 新疆: '新疆维吾尔自治区',
    香港: '香港特别行政区', 澳门: '澳门特别行政区',
  }
  const normalizeProvinceName = (raw) => {
    const name = (raw || '').trim()
    if (!name) return ''
    if (/省|市|自治区|特别行政区/.test(name)) return name
    if (PROVINCE_FULL_NAMES[name]) return PROVINCE_FULL_NAMES[name]
    return `${name}省`
  }

  const offsiteProvinceAgg = useMemo(() => {
    const map = {}
    ;(data.offsiteSales || []).forEach((r) => {
      const province = normalizeProvinceName(r.province)
      if (!province) return
      map[province] = (map[province] || 0) + Number(r.quantity || 0)
    })
    return Object.entries(map)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
  }, [data.offsiteSales])

  const offsiteTopProvinces = useMemo(
    () => offsiteProvinceAgg.slice(0, 8),
    [offsiteProvinceAgg],
  )

  // 平方根缩放：避免最大值（山西 8060）把小销量省份压得看不出颜色
  const offsiteMapData = useMemo(
    () => offsiteProvinceAgg.map((i) => ({ name: i.name, value: Math.sqrt(i.value), realValue: i.value })),
    [offsiteProvinceAgg],
  )
  const offsiteMapMax = useMemo(
    () => Math.sqrt(Math.max(1, ...offsiteProvinceAgg.map((i) => i.value))),
    [offsiteProvinceAgg],
  )

  const offsiteMapOption = useMemo(
    () => ({
      tooltip: {
        trigger: 'item',
        appendToBody: true,
        confine: true,
        textStyle: { fontSize: 11, color: '#e0e6ed', lineHeight: 13 },
        padding: 0,
        borderWidth: 0,
        backgroundColor: 'transparent',
        shadowBlur: 0,
        extraCssText: 'white-space:nowrap;background:transparent;border:none;box-shadow:none !important;',
        formatter: (p) => {
          const real = p.data?.realValue
          if (real == null || Number(real) <= 0) return ''
          return `${p.name}<br/>异地销量：${Number(real).toLocaleString('zh-CN')}`
        },
      },
      visualMap: {
        min: 0,
        max: offsiteMapMax,
        left: 4,
        bottom: 6,
        text: ['高 High', '低 Low'],
        textStyle: {
          color: '#8c9bb0',
          fontSize: 11,
          fontWeight: 500,
        },
        inRange: { color: ['#1a3a5e', '#3a7ab0', '#6fb3d8', '#c9a227'] },
        calculable: false,
        itemWidth: 12,
        itemHeight: 80,
        borderColor: 'rgba(120, 170, 230, 0.18)',
        borderWidth: 1,
        shadowColor: 'rgba(0, 0, 0, 0.35)',
        shadowBlur: 6,
      },
      series: [
        {
          type: 'map',
          map: 'china',
          roam: true,
          scaleLimit: { min: 0.8, max: 3 },
          zoom: 1.05,
          layoutCenter: ['50%', '50%'],
          layoutSize: '98%',
          aspectScale: 0.82,
          itemStyle: {
            areaColor: '#132843',
            borderColor: 'rgba(120, 180, 255, 0.22)',
            borderWidth: 0.8,
            shadowColor: 'rgba(0, 0, 0, 0.45)',
            shadowBlur: 8,
          },
          label: { show: false },
          emphasis: {
            label: {
              show: true,
              color: '#fff',
              fontWeight: 700,
              fontSize: 13,
              textShadow: '0 1px 3px rgba(0,0,0,0.65), 0 0 6px rgba(0,0,0,0.4)',
              backgroundColor: 'rgba(8, 20, 40, 0.85)',
              padding: [3, 6],
              borderRadius: 4,
            },
            itemStyle: {
              areaColor: '#e0b838',
              borderColor: 'rgba(255, 250, 243, 0.9)',
              borderWidth: 1.2,
              shadowColor: 'rgba(224, 184, 56, 0.55)',
              shadowBlur: 16,
            },
          },
          data: offsiteMapData,
        },
      ],
    }),
    [offsiteMapData, offsiteMapMax],
  )

  const retentionColumns = [
    {
      title: (
        <span className={styles.colBi}>
          客户<span>Customer</span>
        </span>
      ),
      dataIndex: 'customerName',
      ellipsis: true,
    },
    {
      title: (
        <span className={styles.colBi}>
          未复购<span>Days</span>
        </span>
      ),
      dataIndex: 'daysSincePurchase',
      width: 88,
      align: 'right',
      render: (v) => {
        const d = v || 0
        const cls = d >= 90 ? styles.daysBadgeHigh : d >= 70 ? styles.daysBadgeMid : ''
        return <span className={`${styles.daysBadge}${cls ? ' ' + cls : ''}`}>{d}天</span>
      },
    },
    {
      title: (
        <span className={styles.colBi}>
          最近购买<span>Last Buy</span>
        </span>
      ),
      dataIndex: 'lastPurchaseDate',
      width: 110,
      render: (v) => v || '-',
    },
  ]

  const inventoryColumnsCompact = [
    {
      title: (
        <span className={styles.colBi}>
          品名<span>Product</span>
        </span>
      ),
      dataIndex: 'productName',
      ellipsis: true,
    },
    {
      title: (
        <span className={styles.colBi}>
          数量<span>Qty</span>
        </span>
      ),
      dataIndex: 'quantity',
      width: 78,
      align: 'right',
      render: (v) => <span className={styles.money}>{MONEY(v)}</span>,
    },
    {
      title: (
        <span className={styles.colBi}>
          金额<span>Amount</span>
        </span>
      ),
      dataIndex: 'amount',
      width: 88,
      align: 'right',
      render: (v) => MONEY(v),
    },
  ]

  const customerColumns = [
    {
      title: (
        <span className={styles.colBi}>
          月份<span>Month</span>
        </span>
      ),
      dataIndex: 'openMonth',
      width: 64,
      render: (v) => {
        if (!v) return '-'
        const [, m] = String(v).split('-')
        return m ? `${Number(m)}月` : v
      },
    },
    {
      title: (
        <span className={styles.colBi}>
          客户<span>Customer</span>
        </span>
      ),
      dataIndex: 'name',
      ellipsis: true,
    },
    {
      title: (
        <span className={styles.colBi}>
          开发额<span>Amount</span>
        </span>
      ),
      dataIndex: 'amount',
      width: 96,
      align: 'right',
      render: (v) => <span className={styles.money}>{MONEY(v)}</span>,
    },
  ]

  const chartFill = { height: '100%', width: '100%', minHeight: 0 }
  const offsiteMax = offsiteTopProvinces[0]?.value || 1
  // 未复购：按天数从小到大排序（刚满60天的在前）
  const retentionSorted = [...(data.retentionAlerts || [])].sort((a, b) => (a.daysSincePurchase || 0) - (b.daysSincePurchase || 0))
  const retentionRows = retentionSorted.slice(0, 8)
  const inventoryRows = (data.inventory || []).slice(0, 8)
  const customerRows = (data.customerDev || []).slice(0, 6)
  const priceList = (priceRows || []).slice(0, 6)

  // 汇总数据
  const invTotalQty = (data.inventory || []).reduce((s, r) => s + Number(r.quantity || 0), 0)
  const invTotalAmt = (data.inventory || []).reduce((s, r) => s + Number(r.amount || 0), 0)
  const custTotal = (data.customerDev || []).length
  const custTotalAmt = (data.customerDev || []).reduce((s, r) => s + Number(r.amount || 0), 0)
  const offsiteTotalQty = (data.offsiteSales || []).reduce((s, r) => s + Number(r.quantity || 0), 0)
  const offsiteTotalAmt = (data.offsiteSales || []).reduce((s, r) => s + Number(r.amount || 0), 0)
  const trendTotal = (data.onlineSaleTrend || []).reduce((s, r) => s + Number(r.saleAmount || 0), 0)
  const rankTotal = (data.salesRank || []).reduce((s, r) => s + Number(r.amount || 0), 0)
  const retentionTotal = data.summary?.retentionAlertCount || (data.retentionAlerts || []).length
  const priceCrawlTime = priceRows?.[0]?.crawlTime
  const structureTotal = (data.productStructure || []).reduce((s, r) => s + Number(r.quantity || 0), 0)

  return (
    <div ref={pageRef} className={`${styles.page} ${fullscreen ? styles.fullscreen : ''} ${styles.oneScreen}`}>
      <Spin spinning={loading} className={styles.boardSpin} wrapperClassName={styles.boardSpinWrap}>
        <div className={styles.board}>
          {/* 左侧竖排标题栏 */}
          <aside className={styles.sideBar}>
            <div className={styles.sideBadge}>
              <span className={styles.sideBadgeZh}>经营数据看板</span>
              <span className={styles.sideBadgeEn}>Operations Dashboard</span>
            </div>
            <div className={styles.sideTitleWrap}>
              <span className={styles.sideTitleMain}>{title || '汾源酒业经营体'}</span>
              <span className={styles.sideTitleEn}>Fenyuan Liquor Operations Board</span>
              <span className={styles.sideTitleSub}>客户维护 · 比价 · 异地销售一屏总览</span>
            </div>
            <div className={styles.sideFooter}>
              <div className={styles.sideTime}>
                <span>数据更新</span>
                <span>UPDATED</span>
                <span className={styles.sideTimeVal}>{updatedAt || '--'}</span>
              </div>
              <Button
                type="primary"
                icon={fullscreen ? <CompressOutlined /> : <ExpandOutlined />}
                onClick={toggleFullscreen}
                className={styles.sideBtn}
              >
                {fullscreen ? '退出' : '全屏'}
              </Button>
              <Button
                type="primary"
                icon={<ReloadOutlined />}
                loading={loading}
                onClick={load}
                className={styles.sideBtn}
              >
                刷新
              </Button>
            </div>
          </aside>

          {/* 右侧面板网格 */}
          <div className={styles.mainGrid}>
            {/* 左列：产品结构 / 库存 / 客户开发 */}
            <section className={`${styles.panel} ${styles.colLeft1}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="销售产品结构" en="Product Mix" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/product-structure')}>
                  共计销售 {MONEY(structureTotal)} 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                {(data.productStructure || []).length ? (
                  <ReactECharts
                    key={`structure-${chartKey}`}
                    option={structureOption}
                    style={chartFill}
                    opts={{ renderer: 'canvas' }}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无产品结构数据 No Product Data" />
                )}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.colLeft2}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="库存概况" en="Inventory" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/inventory')}>
                  共计剩余 {MONEY(invTotalQty)} 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                <Table
                  size="small"
                  rowKey={(r) => `${r.productName}-${r.warehouse}`}
                  pagination={false}
                  columns={inventoryColumnsCompact}
                  dataSource={inventoryRows}
                  locale={{ emptyText: '暂无库存 No Inventory' }}
                />
              </div>
            </section>

            <section className={`${styles.panel} ${styles.colLeft3}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="客户开发" en="Customer Dev" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/customer-dev')}>
                  共开发 {custTotal} 个客户 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                <Table
                  size="small"
                  rowKey={(r) => `${r.openMonth}-${r.name}`}
                  pagination={false}
                  columns={customerColumns}
                  dataSource={customerRows}
                  locale={{ emptyText: '暂无新开客户 No New Customers' }}
                />
              </div>
            </section>

            {/* 中列：地图 / 销售趋势 */}
            <section className={`${styles.panel} ${styles.colCenter1}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="异地销售地图" en="Offsite Sales Map" />
                <button
                  type="button"
                  className={styles.linkBtn}
                  onClick={() => navigate('/business/offsite-sales')}
                >
                  共计 {MONEY(offsiteTotalQty)} 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                {mapReady && offsiteProvinceAgg.length ? (
                  <div className={styles.mapBody}>
                    <div className={styles.mapChart}>
                      <ReactECharts
                        ref={mapChartRef}
                        key={`offsite-${chartKey}`}
                        option={offsiteMapOption}
                        style={chartFill}
                        opts={{ renderer: 'canvas' }}
                        onEvents={onMapEvents}
                      />
                    </div>
                  </div>
                ) : (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={mapReady ? '暂无异地销售数据 No Data' : '地图加载中… Loading Map'}
                  />
                )}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.colCenter2}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="在线销售趋势" en="Online Sales Trend" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/online-sale')}>
                  共计 {MONEY(trendTotal)} 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                {(data.onlineSaleTrend || []).length ? (
                  <ReactECharts
                    key={`trend-${chartKey}`}
                    option={trendOption}
                    style={chartFill}
                    opts={{ renderer: 'canvas' }}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无销售数据 No Sales Data" />
                )}
              </div>
            </section>

            {/* 右列：价格对比 / 经销商排名 / 未复购 */}
            <section className={`${styles.panel} ${styles.colRight1}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="价格对比" en="Price Compare" />
                <span className={styles.priceUpdateTime}>{priceCrawlTime || '--'}</span>
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/price-compare')}>
                  实时价格 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                {priceList.length ? (
                  <div style={{ overflowY: 'auto', height: '100%' }}>
                    <div className={styles.priceHeader}>
                      <span>产品</span>
                      <span className={styles.priceHeaderFy}>汾源</span>
                      <span className={styles.priceHeaderJd}>美团</span>
                      <span className={styles.priceHeaderTmall}>天猫</span>
                    </div>
                    {priceList.map((r) => (
                      <div key={r.id} className={styles.priceRow}>
                        <span className={styles.priceRowName}>{r.productName}</span>
                        <span className={`${styles.priceFy}${r.salePrice ? '' : ' price-missing'}`}>{r.salePrice ? `¥${MONEY(r.salePrice)}` : '-'}</span>
                        <span className={`${styles.priceJd}${r.jdPrice ? '' : ' price-missing'}`}>{r.jdPrice ? `¥${MONEY(r.jdPrice)}` : '-'}</span>
                        <span className={`${styles.priceTmall}${r.tmallPrice ? '' : ' price-missing'}`}>{r.tmallPrice ? `¥${MONEY(r.tmallPrice)}` : '-'}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无价格数据 No Price Data" />
                )}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.colRight2}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="经销商销售排名" en="Dealer Sales Rank" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/sales-rank')}>
                  共计销售 {MONEY(rankTotal)} 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                {(data.salesRank || []).length ? (
                  <ReactECharts
                    key={`rank-${chartKey}`}
                    option={rankOption}
                    style={chartFill}
                    opts={{ renderer: 'canvas' }}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无排名数据 No Rank Data" />
                )}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.colRight3}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="60天未复购客户" en="Inactive Customers 60d" />
                <button
                  type="button"
                  className={styles.linkBtn}
                  onClick={() => navigate('/business/customer-maintain')}
                >
                  共计 {retentionTotal} 客户 详情查看→
                </button>
              </header>
              <div className={styles.panelBody}>
                <Table
                  size="small"
                  rowKey={(r) => `${r.customerName}-${r.daysSincePurchase}`}
                  pagination={false}
                  columns={retentionColumns}
                  dataSource={retentionRows}
                  locale={{ emptyText: '暂无预警客户 No Alerts' }}
                />
              </div>
            </section>
          </div>
        </div>
      </Spin>
    </div>
  )
}
