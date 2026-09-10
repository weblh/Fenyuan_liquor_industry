import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Empty, Space, Spin, Table, Tooltip, message } from 'antd'
import {
  AlertOutlined,
  CompressOutlined,
  DatabaseOutlined,
  EnvironmentOutlined,
  ExpandOutlined,
  FundOutlined,
  MoneyCollectOutlined,
  ReloadOutlined,
  ShoppingCartOutlined,
  SolutionOutlined,
  TrophyOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import { useSelector } from 'react-redux'
import { getDashboardOverview } from '@/api'
import { customerMaintainApi } from '@/api/modules/business'
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

/** 把任意形式的省份名（简称/全称）归一化成地图 feature 的精确名称 */
const PROVINCE_NORMALIZE = (() => {
  const FULL = [
    '北京市', '天津市', '上海市', '重庆市',
    '河北省', '山西省', '辽宁省', '吉林省', '黑龙江省',
    '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省',
    '河南省', '湖北省', '湖南省', '广东省', '海南省',
    '四川省', '贵州省', '云南省', '陕西省', '甘肃省', '青海省', '台湾省',
    '内蒙古自治区', '广西壮族自治区', '西藏自治区', '宁夏回族自治区', '新疆维吾尔自治区',
    '香港特别行政区', '澳门特别行政区',
  ]
  const map = {}
  FULL.forEach((full) => {
    map[full] = full
    // 去掉省/市/自治区/特别行政区
    const s1 = full.replace(/省|市|自治区|特别行政区/g, '')
    map[s1] = full
    // 去掉民族词
    const s2 = full.replace(/壮族|回族|维吾尔/g, '')
    map[s2] = full
    // 同时去掉
    const s3 = full.replace(/省|市|自治区|特别行政区|壮族|回族|维吾尔/g, '')
    map[s3] = full
  })
  return (raw) => map[raw] || raw
})()

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

export default function SubDashboard() {
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
  const pageRef = useRef(null)

  /** 把一个分页资源的所有页全部拉完，返回合并后的 records 数组
   *  分批并行（每批 8 页）避免一次性打爆后端 */
  const fetchAllPages = useCallback(async (api, pageSize = 10, batchSize = 8) => {
    const first = await api.list({ current: 1, pageSize })
    const firstRecords = Array.isArray(first) ? first : first?.records || first?.list || []
    const total = Number(first?.total ?? firstRecords.length)
    const pages = Number(first?.pages ?? Math.max(1, Math.ceil(total / pageSize)))

    if (pages <= 1) return firstRecords

    // 分批拉取剩余页（第 2 页 ~ 第 pages 页）
    const rest = []
    for (let from = 2; from <= pages; from += batchSize) {
      const chunk = Array.from({ length: Math.min(batchSize, pages - from + 1) }, (_, i) =>
        api.list({ current: from + i, pageSize }).then((res) =>
          Array.isArray(res) ? res : res?.records || res?.list || []
        )
      )
      const results = await Promise.all(chunk)
      rest.push(...results)
    }
    return [...firstRecords, ...rest.flat()]
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      // 并行获取大屏概览 + 客户维护全部分页
      const [overview, allCustomers] = await Promise.all([
        getDashboardOverview().catch(() => null),
        fetchAllPages(customerMaintainApi, 50).catch(() => null),
      ])

      const merged = overview || EMPTY_DATA

      // 从全量客户里筛选出 60 天未复购的预警客户（后端可能返回的是全部客户，不限于预警）
      if (allCustomers && allCustomers.length > 0) {
        const alertOnly = allCustomers.filter(
          (c) => Number(c.daysSincePurchase) >= 60
        )
        merged.retentionAlerts = alertOnly
        if (merged.summary) {
          merged.summary = { ...merged.summary, retentionAlertCount: alertOnly.length }
        }
      }

      setData(merged)
      setUpdatedAt(formatNow())
    } catch {
      setData(EMPTY_DATA)
    } finally {
      setLoading(false)
    }
  }, [fetchAllPages])

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

  const summary = data.summary || {}

  const kpiCards = [
    {
      key: 'retention',
      label: '60天未复购',
      labelEn: 'Inactive 60d',
      value: summary.retentionAlertCount || 0,
      tip: '超60天未复购客户，已推送主看板',
      icon: <AlertOutlined />,
      tone: 'crimson',
      path: '/business/customer-maintain',
      suffix: '家',
      suffixEn: 'Clients',
    },
    {
      key: 'price',
      label: '价格对比',
      labelEn: 'Price Compare',
      value: '京东/天猫',
      tip: '自动抓取平台价与销售价对比',
      icon: <FundOutlined />,
      tone: 'amber',
      path: '/business/price-compare',
    },
    {
      key: 'offsite',
      label: '异地销量',
      labelEn: 'Offsite Sales',
      value: MONEY(summary.totalOffsiteQty),
      tip: `覆盖 ${summary.offsiteProvinceCount || 0} 个省份`,
      icon: <EnvironmentOutlined />,
      tone: 'slate',
      path: '/business/offsite-sales',
    },
    {
      key: 'sale',
      label: '在线销售额',
      labelEn: 'Online Sales',
      value: MONEY(summary.totalSaleAmount),
      tip: 'ERP「美福」+「即时零售」(美团) 分列',
      icon: <ShoppingCartOutlined />,
      tone: 'wine',
      path: '/business/online-sale',
    },
    {
      key: 'payment',
      label: '回款金额',
      labelEn: 'Collections',
      value: MONEY(summary.totalPaymentAmount),
      tip: '各周期回款金额合计',
      icon: <MoneyCollectOutlined />,
      tone: 'amber',
      path: '/business/online-sale',
    },
    {
      key: 'inventory',
      label: Number(summary.totalInventoryAmount) > 0 ? '库存金额' : '库存数量',
      labelEn: Number(summary.totalInventoryAmount) > 0 ? 'Inventory Amt' : 'Inventory Qty',
      value: Number(summary.totalInventoryAmount) > 0
        ? MONEY(summary.totalInventoryAmount)
        : MONEY(summary.totalInventoryQty),
      tip: `SKU ${summary.inventorySkuCount || 0} · 金额 ${MONEY(summary.totalInventoryAmount)} · 数量 ${MONEY(summary.totalInventoryQty)}`,
      icon: <DatabaseOutlined />,
      tone: 'slate',
      path: '/business/inventory',
    },
    {
      key: 'customer',
      label: '客户开发额',
      labelEn: 'Customer Dev',
      value: MONEY(summary.totalCustomerDevAmount),
      tip: `${summary.customerDevCount || 0} 家客户`,
      icon: <SolutionOutlined />,
      tone: 'olive',
      path: '/business/customer-dev',
    },
    {
      key: 'dealer',
      label: '经销商排名',
      labelEn: 'Dealer Rank',
      value: summary.dealerCount || 0,
      tip: '销售排名收录家数',
      icon: <TrophyOutlined />,
      tone: 'bronze',
      path: '/business/sales-rank',
      suffix: '家',
      suffixEn: 'Dealers',
    },
  ]

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
      color: ['#2f80ed', '#28c7e6'],
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'transparent',
        borderColor: 'transparent',
        borderWidth: 0,
        padding: 0,
        textStyle: {
          fontSize: 12,
          color: '#ffffff',
          textShadowColor: 'rgba(0, 0, 0, 0.85)',
          textShadowBlur: 4,
        },
        extraCssText: 'background: transparent !important; border: none !important; box-shadow: none !important;',
        position: (point) => [point[0] + 14, point[1] - 10],
        axisPointer: { type: 'none' },
        formatter: (params) => {
          if (!Array.isArray(params) || !params.length) return ''
          const idx = params[0].dataIndex
          const lines = params.map((p) => {
            const channel = String(p.seriesName || '').replace('销售额', '')
            if (idx <= 0) {
              return `${channel}：暂无上月数据`
            }
            const prev = Number(byKey[`${channel}|${periods[idx - 1]}`] || 0)
            if (prev <= 0) {
              return `${channel}：上月无数据`
            }
            const cur = Number(p.value || 0)
            const rate = ((cur - prev) / prev) * 100
            const up = rate >= 0
            const color = up ? '#56d8ee' : '#ff7a7a'
            const word = up ? '增长' : '减少'
            return `${channel}：相比于上月${word} <b style="color:${color};text-shadow:0 0 3px rgba(0,0,0,.9)">${Math.abs(rate).toFixed(1)}%</b>`
          })
          return lines.join('<br/>')
        },
      },
      legend: {
        data: channels.map((c) => `${c}销售额`),
        top: 2,
        right: 4,
        itemWidth: 13,
        itemHeight: 9,
        itemGap: 18,
        icon: 'roundRect',
        textStyle: { color: '#9db2d0', fontSize: 11.5, fontWeight: 500 },
      },
      grid: { left: 48, right: 16, top: 40, bottom: 28 },
      xAxis: {
        type: 'category',
        data: periods.map(monthLabel),
        axisLine: { lineStyle: { color: '#2a3f63' } },
        axisTick: { show: false },
        axisLabel: { color: '#8fa3c4', fontSize: 12, fontWeight: 500, margin: 11 },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: '#7d92b5',
          fontSize: 11,
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        splitLine: { lineStyle: { type: 'dashed', color: 'rgba(120, 160, 220, 0.12)' } },
      },
      series: channels.map((channel, idx) => {
        const isWine = idx === 0
        const lineColor = isWine ? '#5aa5ff' : '#56d8ee'
        return {
          name: `${channel}销售额`,
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 7,
          showSymbol: true,
          data: periods.map((p) => byKey[`${channel}|${p}`] || 0),
          lineStyle: {
            width: 3,
            color: lineColor,
            shadowColor: isWine ? 'rgba(47, 128, 237, 0.5)' : 'rgba(34, 211, 238, 0.5)',
            shadowBlur: 8,
          },
          itemStyle: {
            color: lineColor,
            borderColor: '#0b1729',
            borderWidth: 2,
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: isWine ? 'rgba(90, 165, 255, 0.35)' : 'rgba(86, 216, 238, 0.32)' },
                { offset: 1, color: 'rgba(13, 26, 48, 0)' },
              ],
            },
          },
          emphasis: {
            focus: 'series',
            itemStyle: {
              borderWidth: 3,
              shadowBlur: 12,
              shadowColor: lineColor,
            },
          },
          label: {
            show: true,
            position: 'top',
            distance: 6,
            color: lineColor,
            fontSize: 11,
            fontWeight: 700,
            formatter: (p) => (p.value ? MONEY(p.value) : ''),
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
    const chartData = Object.entries(seriesMap).map(([name, value]) => ({ name, value }))
    const totalQty = chartData.reduce((s, i) => s + Number(i.value || 0), 0)
    const esc = (s) => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
    // 每款酒：内亮外深的径向渐变，环面立体饱满
    const palette = [
      ['#5aa5ff', '#1e5fb8'],
      ['#56d8ee', '#1497c4'],
      ['#4fe0cc', '#1a9c8c'],
      ['#8fa8ff', '#4a5fd0'],
      ['#6fb7ff', '#2f7fd9'],
      ['#3dd6f2', '#0f8fb8'],
    ]
    const gradientData = chartData.map((d, i) => {
      const [light, dark] = palette[i % palette.length]
      return {
        ...d,
        itemStyle: {
          color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.8, [
            { offset: 0, color: light },
            { offset: 1, color: dark },
          ]),
        },
      }
    })
    return {
      tooltip: {
        trigger: 'item',
        appendToBody: true,
        backgroundColor: 'rgba(13, 26, 48, 0.97)',
        borderColor: 'rgba(90, 165, 255, 0.35)',
        borderWidth: 1,
        padding: [10, 14],
        textStyle: { fontSize: 12.5, color: '#d5e2f5' },
        extraCssText: 'box-shadow: none; border-radius: 10px;',
        formatter: (p) => {
          try {
            const qty = Number(p.value || 0)
            let html = `<div style="font-weight:700;font-size:13px;color:#eaf2ff;margin-bottom:4px;white-space:nowrap;">${esc(p.name)}</div>`
            html += `<div style="color:#7cb8ff;font-weight:700;line-height:19px;">销量：${qty.toLocaleString('zh-CN')} 箱</div>`
            html += `<div style="color:#56d8ee;font-weight:700;line-height:19px;">占比：${p.percent}%</div>`
            return html
          } catch (e) {
            return `${p.name}<br/>占比：${p.percent}%<br/>销量：${p.value} 箱`
          }
        },
      },
      legend: { show: false },
      title: {
        text: `{v|${totalQty.toLocaleString('zh-CN')}}\n{u|总销量（箱）TOTAL}`,
        left: 'center',
        top: 'center',
        textStyle: {
          rich: {
            v: { fontSize: 26, fontWeight: 800, color: '#7cb8ff', lineHeight: 32, fontVariantNumeric: 'tabular-nums' },
            u: { fontSize: 10, fontWeight: 600, color: '#7d92b5', letterSpacing: 1.5, lineHeight: 16 },
          },
        },
      },
      series: [
        {
          type: 'pie',
          radius: ['52%', '79%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 0,
            borderWidth: 2,
            borderColor: 'rgba(10, 24, 46, 0.9)',
            shadowColor: 'rgba(0, 0, 0, 0.4)',
            shadowBlur: 12,
            shadowOffsetY: 4,
          },
          label: {
            show: true,
            position: 'outside',
            color: '#eaf2ff',
            fontSize: 11,
            fontWeight: 500,
            lineHeight: 1.4,
            formatter: (p) => String(p.name || '').replace(/^.*?\s*·\s*/, ''),
          },
          labelLine: {
            show: true,
            length: 8,
            length2: 10,
            smooth: true,
            lineStyle: {
              color: 'rgba(140, 195, 255, 0.5)',
              width: 1,
            },
          },
          labelLayout: {
            hideOverlap: true,
          },
          emphasis: {
            scale: true,
            scaleSize: 6,
            label: {
              show: true,
              fontSize: 13,
              fontWeight: 700,
              color: '#ffffff',
              textShadow: '0 1px 4px rgba(0,0,0,0.6)',
            },
            itemStyle: {
              borderColor: 'rgba(140, 195, 255, 0.9)',
              borderWidth: 2,
              shadowBlur: 22,
              shadowColor: 'rgba(47, 128, 237, 0.5)',
            },
          },
          data: gradientData,
        },
      ],
    }
  }, [data.productStructure])

  const rankOption = useMemo(() => {
    const full = [...(data.salesRank || [])]
    const totalAmount = full.reduce((s, i) => s + Number(i.amount || 0), 0)
    const list = full.slice(0, 8).reverse()
    const n = list.length
    const barColor = (rankFromTop) => {
      // 横向渐变（左→右）
      if (rankFromTop === 1) {
        return {
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#7ce8fa' },
            { offset: 1, color: '#1497c4' },
          ],
        }
      }
      if (rankFromTop <= 3) {
        return {
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#5aa5ff' },
            { offset: 1, color: '#1e5fb8' },
          ],
        }
      }
      return {
        type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [
          { offset: 0, color: '#4d7fc0' },
          { offset: 1, color: '#2f5d9e' },
        ],
      }
    }
    return {
      tooltip: { show: false },
      grid: { left: 92, right: 58, top: 10, bottom: 10 },
      xAxis: {
        type: 'value',
        axisLabel: {
          color: '#7d92b5',
          fontSize: 11,
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { type: 'dashed', color: 'rgba(120, 160, 220, 0.12)' } },
      },
      yAxis: {
        type: 'category',
        data: list.map((i) => i.companyName),
        axisLabel: {
          color: '#d3def0',
          width: 82,
          overflow: 'truncate',
          fontSize: 12.5,
          fontWeight: 600,
          margin: 12,
        },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          type: 'bar',
          barMaxWidth: 15,
          barCategoryGap: '42%',
          data: list.map((i, idx) => {
            const rankFromTop = n - idx
            const gold = rankFromTop === 1
            return {
              value: Number(i.amount || 0),
              itemStyle: {
                borderRadius: [0, 999, 999, 0],
                color: barColor(rankFromTop),
                shadowColor: gold ? 'rgba(34, 211, 238, 0.4)' : 'rgba(47, 128, 237, 0.3)',
                shadowBlur: 6,
                shadowOffsetX: 2,
              },
            }
          }),
          label: {
            show: true,
            position: 'right',
            distance: 8,
            color: '#8fc9ff',
            fontSize: 12,
            fontWeight: 700,
            fontFamily: 'inherit',
            formatter: (p) => MONEY(p.value),
          },
          emphasis: {
            focus: 'none',
            itemStyle: { shadowBlur: 14 },
            label: {
              show: true,
              color: '#ffffff',
              fontSize: 13,
              fontWeight: 800,
              formatter: (p) => {
                const amt = Number(p.value || 0)
                const pct = totalAmount > 0 ? ((amt / totalAmount) * 100).toFixed(1) : '0.0'
                return `${pct}%`
              },
            },
          },
        },
      ],
    }
  }, [data.salesRank])

  const { offsiteProvinceAgg, provinceProductsMap } = useMemo(() => {
    const aggMap = {}
    const prodMap = {}
    ;(data.offsiteSales || []).forEach((r) => {
      const province = PROVINCE_NORMALIZE((r.province || '').trim()) || '未知'
      const qty = Number(r.quantity || 0)
      const product = (r.productName || '未命名').trim()
      aggMap[province] = (aggMap[province] || 0) + qty
      if (!prodMap[province]) prodMap[province] = {}
      prodMap[province][product] = (prodMap[province][product] || 0) + qty
    })
    const agg = Object.entries(aggMap)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
    // 把 prodMap 的内部结构也转成数组，便于 tooltip 排序
    const productListMap = {}
    Object.entries(prodMap).forEach(([prov, obj]) => {
      productListMap[prov] = Object.entries(obj)
        .map(([product, qty]) => ({ product, qty }))
        .sort((a, b) => b.qty - a.qty)
    })
    return { offsiteProvinceAgg: agg, provinceProductsMap: productListMap }
  }, [data.offsiteSales])

  const offsiteMapOption = useMemo(
    () => {
      const maxVal = Math.max(10, ...offsiteProvinceAgg.map((i) => i.value), 0)
      return {
        tooltip: {
          trigger: 'item',
          appendToBody: true,
          backgroundColor: 'rgba(13, 26, 48, 0.97)',
          borderColor: 'rgba(90, 165, 255, 0.35)',
          borderWidth: 1,
          padding: [10, 14],
          textStyle: { fontSize: 12.5, color: '#d5e2f5' },
          extraCssText: 'box-shadow: none; border-radius: 10px;',
          formatter: (p) => {
            try {
              const province = p.name || ''
              const total = Number(p.value || 0)
              const products = provinceProductsMap[province] || []
              const esc = (s) => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
              let html = `<div style="font-weight:700;font-size:13px;color:#eaf2ff;margin-bottom:4px;">${esc(province)}</div>`
              html += `<div style="color:#7cb8ff;font-weight:700;line-height:19px;">异地销量：${total.toLocaleString('zh-CN')}</div>`
              if (products.length > 0) {
                html += `<div style="font-size:10.5px;color:#8fa3c4;letter-spacing:0.04em;border-top:1px dashed rgba(90,165,255,0.25);margin-top:6px;padding-top:5px;">产品明细（共 ${products.length} 款）</div>`
                html += `<table style="border-collapse:collapse;margin-top:2px;width:250px;"><tbody>`
                products.slice(0, 6).forEach((item, idx) => {
                  const dot = idx === 0 ? '#56d8ee' : idx === 1 ? '#5aa5ff' : '#6f87b0'
                  const weight = idx < 2 ? 600 : 500
                  html += `<tr>`
                    + `<td style="padding:2px 0;color:#d5e2f5;font-weight:${weight};font-size:12px;white-space:nowrap;">`
                    + `<span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:${dot};margin-right:6px;vertical-align:middle;"></span>${esc(item.product)}`
                    + `</td>`
                    + `<td style="padding:2px 0 2px 12px;color:#8fc9ff;font-weight:700;font-size:12px;text-align:right;white-space:nowrap;font-variant-numeric:tabular-nums;">${Number(item.qty || 0).toLocaleString('zh-CN')}</td>`
                    + `</tr>`
                })
                html += `</tbody></table>`
                if (products.length > 6) {
                  html += `<div style="color:#7d92b5;font-size:11px;">仅展示前 6 款，共 ${products.length} 款</div>`
                }
              } else {
                html += `<div style="color:#7d92b5;font-size:11px;margin-top:2px;">暂无销售明细</div>`
              }
              return html
            } catch (e) {
              return `${p.name}<br/>异地销量：${Number(p.value || 0).toLocaleString('zh-CN')}`
            }
          },
        },
        visualMap: {
          min: 0,
          max: maxVal,
          left: 14,
          bottom: 18,
          text: ['高 High', '低 Low'],
          textStyle: { color: '#9db2d0', fontSize: 11.5, fontWeight: 500 },
          inRange: { color: ['#12294c', '#1e5fb8', '#2f80ed', '#5fd4f2'] },
          calculable: false,
          itemWidth: 14,
          itemHeight: 100,
          itemGap: 4,
          orient: 'vertical',
        },
        series: [
          {
            type: 'map',
            map: 'china',
            roam: true,
            zoom: 1,
          scaleLimit: { min: 0.6, max: 4 },
          layoutCenter: ['50%', '52%'],
          layoutSize: '100%',
          aspectScale: 0.82,
            selectedMode: false,
            itemStyle: {
              areaColor: '#162849',
              borderColor: '#3a5a90',
              borderWidth: 0.8,
              shadowColor: 'rgba(0, 0, 0, 0.35)',
              shadowBlur: 5,
              shadowOffsetY: 2,
            },
            label: {
              show: true,
              color: '#eaf2ff',
              fontSize: 10.5,
              fontWeight: 500,
              textShadow: '0 1px 3px rgba(0,0,0,0.85)',
              formatter: (p) => {
                if (!p.name || p.name.includes('南海') || p.name.includes('九段')) return ''
                return p.name
              },
            },
            emphasis: {
              label: { show: true, color: '#ffffff', fontWeight: 700, fontSize: 12, textShadow: '0 1px 3px rgba(0,0,0,0.85)' },
              itemStyle: {
                areaColor: '#2f80ed',
                borderColor: '#7cc7ff',
                borderWidth: 1.2,
                shadowBlur: 12,
                shadowColor: 'rgba(47, 128, 237, 0.5)',
              },
            },
            select: {
              label: { color: '#ffffff', fontWeight: 700, textShadow: '0 1px 3px rgba(0,0,0,0.85)' },
              itemStyle: { areaColor: '#2f80ed', borderColor: '#7cc7ff' },
            },
            data: offsiteProvinceAgg,
          },
        ],
      }
    },
    [offsiteProvinceAgg, provinceProductsMap],
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
      width: 92,
      align: 'right',
      render: (v) => {
        const d = Number(v) || 0
        const tier = d >= 235 ? 'badgeHot' : d >= 180 ? 'badgeHigh' : d >= 120 ? 'badgeWarn' : 'badgeMild'
        return (
          <span className={`${styles.daysBadge} ${styles[tier]}`}>
            <i className={styles.badgeDot} />
            {d}<em>天</em>
          </span>
        )
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
      render: (v) => <span className={styles.dateCell}>{v || '-'}</span>,
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
  const retentionRows = [...(data.retentionAlerts || [])]
    .sort((a, b) => (Number(a.daysSincePurchase) || 0) - (Number(b.daysSincePurchase) || 0))
    .slice(0, 6)
  const inventoryRows = (data.inventory || []).slice(0, 8)
  const customerRows = (data.customerDev || []).slice(0, 6)

  return (
    <div ref={pageRef} className={`${styles.page} ${fullscreen ? styles.fullscreen : ''} ${styles.oneScreen}`}>
      <div className={styles.hero} style={{ display: 'none' }} />

      <Spin spinning={loading} className={styles.boardSpin} wrapperClassName={styles.boardSpinWrap}>
        <div className={styles.board}>
          <div className={styles.mainGrid}>
            <div className={styles.kpiGrid}>
            {kpiCards
              .filter(
                (card) =>
                  card.key !== 'retention' &&
                  card.key !== 'sale' &&
                  card.key !== 'offsite' &&
                  card.key !== 'inventory' &&
                  card.key !== 'dealer' &&
                  card.key !== 'customer'
              )
              .map((card) => (
              <Tooltip key={card.key} title={card.tip}>
                <button
                  type="button"
                  className={`${styles.kpiCard} ${styles[card.tone]}`}
                  onClick={() => navigate(card.path)}
                >
                  <div className={styles.kpiIcon}>{card.icon}</div>
                  <div className={styles.kpiBody}>
                    <div className={styles.kpiLabel}>
                      <span className={styles.biZh}>{card.label}</span>
                      {card.labelEn ? <span className={styles.biEn}>{card.labelEn}</span> : null}
                    </div>
                    <div className={styles.kpiValue}>
                      {card.value}
                      {card.suffix ? (
                        <small>
                          {card.suffix}
                          {card.suffixEn ? <span className={styles.suffixEn}>{card.suffixEn}</span> : null}
                        </small>
                      ) : null}
                    </div>
                  </div>
                </button>
              </Tooltip>
            ))}
            </div>

            <section className={`${styles.panel} ${styles.panelHero}`}>
              <div className={styles.heroText}>
                <BiTitle
                  as="div"
                  className={styles.eyebrow}
                  zh="经营数据看板"
                  en="Operations Dashboard"
                />
                <h1>
                  <span className={styles.biZh}>{title || '汾源酒业经营体'}</span>
                  <span className={styles.heroEn}>Fenyuan Liquor Operations Board</span>
                </h1>
                <p>
                  <span className={styles.heroZh}>你好，{name}。客户维护 · 比价 · 异地销售一屏总览</span>
                  <span className={styles.heroSubEn}>Customer · Pricing · Offsite Sales at a Glance</span>
                </p>
              </div>
              <div className={styles.heroMeta}>
                <span className={styles.metaLine}>
                  <span className={styles.metaZh}>数据更新</span>
                  <span className={styles.metaEn}>Updated</span>
                  <span className={styles.metaVal}>{updatedAt || '--'}</span>
                </span>
                <Space size={8} className={styles.heroActions}>
                  <Button
                    type="primary"
                    icon={fullscreen ? <CompressOutlined /> : <ExpandOutlined />}
                    onClick={toggleFullscreen}
                    className={styles.refreshBtn}
                  >
                    {fullscreen ? '退出全屏 Exit' : '全屏 Full Screen'}
                  </Button>
                  <Button
                    type="primary"
                    icon={<ReloadOutlined />}
                    loading={loading}
                    onClick={load}
                    className={styles.refreshBtn}
                  >
                    刷新 Refresh
                  </Button>
                </Space>
              </div>
            </section>

            <section className={`${styles.panel} ${styles.panelRetention}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="60天未复购客户" en="Inactive Customers 60d" />
                <button
                  type="button"
                  className={styles.linkBtn}
                  onClick={() => navigate('/business/customer-maintain')}
                >
                  共计{summary.retentionAlertCount || 0}家 · 详情查看 →
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

            <section className={`${styles.panel} ${styles.mapPanel}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="异地销售地图" en="Offsite Sales Map" />
                <button
                  type="button"
                  className={styles.linkBtn}
                  onClick={() => navigate('/business/offsite-sales')}
                >
                  共计{MONEY(summary.totalOffsiteQty)} · 详情查看 →
                </button>
              </header>
              <div className={styles.panelBody}>
                {mapReady && offsiteProvinceAgg.length ? (
                  <div className={styles.mapChart}>
                    <ReactECharts
                      key={`offsite-${chartKey}`}
                      option={offsiteMapOption}
                      style={chartFill}
                      opts={{ renderer: 'canvas' }}
                    />
                  </div>
                ) : (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={mapReady ? '暂无异地销售数据 No Data' : '地图加载中… Loading Map'}
                  />
                )}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.panelOnlineTrend}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="在线销售趋势" en="Online Sales Trend" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/online-sale')}>
                  共计{MONEY(summary.totalSaleAmount)} · 详情查看 →
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

            <section className={`${styles.panel} ${styles.panelProductMix}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="销售产品结构" en="Product Mix" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/product-structure')}>
                  明细 Detail
                </button>
              </header>
              <div className={styles.panelBody}>
                {(data.productStructure || []).length ? (
                  <ReactECharts
                    key={`structure-${chartKey}`}
                    option={structureOption}
                    notMerge
                    style={chartFill}
                    opts={{ renderer: 'canvas' }}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无产品结构数据 No Product Data" />
                )}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.panelRank}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="经销商销售排名" en="Dealer Sales Rank" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/sales-rank')}>
                  共计{summary.dealerCount || 0}家 · 详情查看 →
                </button>
              </header>
              <div className={styles.panelBody}>
                {(data.salesRank || []).length ? (
                  <ReactECharts
                    key={`rank-${chartKey}`}
                    option={rankOption}
                    notMerge
                    style={chartFill}
                    opts={{ renderer: 'canvas' }}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无排名数据 No Rank Data" />
                )}
              </div>
            </section>

            <section className={`${styles.panel} ${styles.panelSplit}`}>
              <div className={styles.splitHalf}>
                <header className={styles.panelHead}>
                  <BiTitle zh="库存概况" en="Inventory" />
                  <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/inventory')}>
                    共计
                    {Number(summary.totalInventoryAmount) > 0
                      ? MONEY(summary.totalInventoryAmount)
                      : MONEY(summary.totalInventoryQty)}{' '}
                    · 详情查看 →
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
              </div>
              <div className={styles.splitHalf}>
                <header className={styles.panelHead}>
                  <BiTitle zh="客户开发" en="Customer Dev" />
                  <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/customer-dev')}>
                    共计{MONEY(summary.totalCustomerDevAmount)} · 详情查看 →
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
              </div>
            </section>
          </div>
        </div>
      </Spin>
    </div>
  )
}
