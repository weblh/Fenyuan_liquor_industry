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

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getDashboardOverview()
      setData(res || EMPTY_DATA)
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
      color: ['#8b1a1a', '#c9a227'],
      tooltip: { trigger: 'axis', textStyle: { fontSize: 13 } },
      legend: {
        data: channels.map((c) => `${c}销售额`),
        top: 0,
        right: 0,
        itemWidth: 14,
        itemHeight: 10,
        textStyle: { color: '#595959', fontSize: 13 },
      },
      grid: { left: 52, right: 14, top: 34, bottom: 26 },
      xAxis: {
        type: 'category',
        data: periods.map(monthLabel),
        axisLine: { lineStyle: { color: '#d9d9d9' } },
        axisLabel: { color: '#8c8c8c', fontSize: 13 },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: '#8c8c8c',
          fontSize: 13,
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
      },
      series: channels.map((channel, idx) => ({
        name: `${channel}销售额`,
        type: 'bar',
        barMaxWidth: 18,
        barGap: '20%',
        data: periods.map((p) => byKey[`${channel}|${p}`] || 0),
        itemStyle: { borderRadius: [3, 3, 0, 0] },
        label: {
          show: true,
          position: 'top',
          color: idx === 0 ? '#8b1a1a' : '#8a7020',
          fontSize: 11,
          fontWeight: 600,
          formatter: (p) => (p.value ? MONEY(p.value) : ''),
        },
      })),
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
    return {
      color: ['#8b1a1a', '#c9a227', '#6b8f71', '#4a6fa5', '#a0522d', '#7a5c4e'],
      tooltip: {
        trigger: 'item',
        textStyle: { fontSize: 13 },
        formatter: (p) => `${p.name}<br/>销量 Qty：${p.value} 箱<br/>占比 Share：${p.percent}%`,
      },
      legend: {
        orient: 'vertical',
        right: 4,
        top: 'middle',
        itemWidth: 12,
        itemHeight: 10,
        textStyle: { color: '#595959', fontSize: 12 },
      },
      series: [
        {
          type: 'pie',
          radius: ['38%', '64%'],
          center: ['36%', '52%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 3, borderColor: '#fff', borderWidth: 1 },
          label: { show: false },
          data: chartData,
        },
      ],
    }
  }, [data.productStructure])

  const rankOption = useMemo(() => {
    const list = [...(data.salesRank || [])].slice(0, 8).reverse()
    return {
      color: ['#8b1a1a'],
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params) => {
          const p = params?.[0]
          if (!p) return ''
          return `${p.name}<br/>销售额：${MONEY(p.value)}`
        },
      },
      grid: { left: 96, right: 56, top: 8, bottom: 8 },
      xAxis: {
        type: 'value',
        axisLabel: {
          color: '#8c8c8c',
          fontSize: 12,
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
      },
      yAxis: {
        type: 'category',
        data: list.map((i) => i.companyName),
        axisLabel: { color: '#595959', width: 84, overflow: 'truncate', fontSize: 13 },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          type: 'bar',
          barMaxWidth: 16,
          data: list.map((i) => Number(i.amount || 0)),
          itemStyle: { borderRadius: [0, 5, 5, 0] },
          label: {
            show: true,
            position: 'right',
            color: '#8c8c8c',
            fontSize: 12,
            formatter: (p) => MONEY(p.value),
          },
        },
      ],
    }
  }, [data.salesRank])

  const offsiteProvinceAgg = useMemo(() => {
    const map = {}
    ;(data.offsiteSales || []).forEach((r) => {
      const province = (r.province || '').replace(/省|市|自治区|壮族|回族|维吾尔/g, '') || '未知'
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

  const offsiteMapOption = useMemo(
    () => ({
      tooltip: {
        trigger: 'item',
        formatter: (p) => `${p.name}<br/>异地销量：${Number(p.value || 0).toLocaleString('zh-CN')}`,
      },
      visualMap: {
        min: 0,
        max: Math.max(10, ...offsiteProvinceAgg.map((i) => i.value), 0),
        left: 2,
        bottom: 4,
        text: ['高 High', '低 Low'],
        textStyle: { color: '#6b625a', fontSize: 12 },
        inRange: { color: ['#f7e8d3', '#d4a84b', '#8b1a1a'] },
        calculable: false,
        itemWidth: 10,
        itemHeight: 72,
      },
      series: [
        {
          type: 'map',
          map: 'china',
          roam: false,
          zoom: 1.05,
          layoutCenter: ['54%', '50%'],
          layoutSize: '92%',
          aspectScale: 0.82,
          itemStyle: {
            areaColor: '#f3ece0',
            borderColor: '#d9cbb6',
            borderWidth: 0.7,
          },
          label: {
            show: true,
            color: '#4a433c',
            fontSize: 11,
            formatter: (p) => {
              // 南海诸岛等小区域不标，避免挤成一团
              if (!p.name || p.name.includes('南海') || p.name.includes('九段')) return ''
              return p.name
            },
          },
          emphasis: {
            label: { show: true, color: '#1f1a17', fontWeight: 600, fontSize: 13 },
            itemStyle: { areaColor: '#e8c56a' },
          },
          data: offsiteProvinceAgg,
        },
      ],
    }),
    [offsiteProvinceAgg],
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
      render: (v) => <span className={styles.daysBadge}>{v || 0}天</span>,
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
  const retentionRows = (data.retentionAlerts || []).slice(0, 8)
  const inventoryRows = (data.inventory || []).slice(0, 8)
  const customerRows = (data.customerDev || []).slice(0, 6)

  return (
    <div ref={pageRef} className={`${styles.page} ${fullscreen ? styles.fullscreen : ''} ${styles.oneScreen}`}>
      <div className={styles.hero}>
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
            你好，{name}。客户维护 · 比价 · 异地销售一屏总览
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
      </div>

      <Spin spinning={loading} className={styles.boardSpin} wrapperClassName={styles.boardSpinWrap}>
        <div className={styles.board}>
          <div className={styles.kpiGrid}>
            {kpiCards.map((card) => (
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

          <div className={styles.mainGrid}>
            <section className={`${styles.panel} ${styles.panelRetention}`}>
              <header className={styles.panelHead}>
                <BiTitle zh="60天未复购客户" en="Inactive Customers 60d" />
                <button
                  type="button"
                  className={styles.linkBtn}
                  onClick={() => navigate('/business/customer-maintain')}
                >
                  明细 Detail
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
                  明细 Detail
                </button>
              </header>
              <div className={styles.panelBody}>
                {mapReady && offsiteProvinceAgg.length ? (
                  <div className={styles.mapBody}>
                    <div className={styles.mapChart}>
                      <ReactECharts
                        key={`offsite-${chartKey}`}
                        option={offsiteMapOption}
                        style={chartFill}
                        opts={{ renderer: 'canvas' }}
                      />
                    </div>
                    <aside className={styles.mapRank}>
                      <div className={styles.mapRankTitle}>
                        <span className={styles.biZh}>省份销量 TOP</span>
                        <span className={styles.biEn}>Province TOP</span>
                      </div>
                      <ul className={styles.mapRankList}>
                        {offsiteTopProvinces.map((item, idx) => (
                          <li key={item.name} className={styles.mapRankItem}>
                            <span className={`${styles.mapRankNo} ${idx < 3 ? styles.mapRankTop : ''}`}>
                              {idx + 1}
                            </span>
                            <span className={styles.mapRankName}>{item.name}</span>
                            <span className={styles.mapRankBarWrap}>
                              <span
                                className={styles.mapRankBar}
                                style={{ width: `${Math.max(8, (item.value / offsiteMax) * 100)}%` }}
                              />
                            </span>
                            <span className={styles.mapRankVal}>{MONEY(item.value)}</span>
                          </li>
                        ))}
                      </ul>
                    </aside>
                  </div>
                ) : (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={mapReady ? '暂无异地销售数据 No Data' : '地图加载中… Loading Map'}
                  />
                )}
              </div>
            </section>

            <section className={styles.panel}>
              <header className={styles.panelHead}>
                <BiTitle zh="在线销售趋势" en="Online Sales Trend" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/online-sale')}>
                  明细 Detail
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

            <section className={styles.panel}>
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
                    style={chartFill}
                    opts={{ renderer: 'canvas' }}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无产品结构数据 No Product Data" />
                )}
              </div>
            </section>

            <section className={styles.panel}>
              <header className={styles.panelHead}>
                <BiTitle zh="经销商销售排名" en="Dealer Sales Rank" />
                <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/sales-rank')}>
                  明细 Detail
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

            <section className={`${styles.panel} ${styles.panelSplit}`}>
              <div className={styles.splitHalf}>
                <header className={styles.panelHead}>
                  <BiTitle zh="库存概况" en="Inventory" />
                  <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/inventory')}>
                    明细 Detail
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
                    明细 Detail
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
