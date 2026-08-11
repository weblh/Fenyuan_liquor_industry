import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Empty, Space, Spin, Table, Tag, Tooltip, message } from 'antd'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  CompressOutlined,
  DatabaseOutlined,
  ExpandOutlined,
  MoneyCollectOutlined,
  ReloadOutlined,
  ShoppingCartOutlined,
  SolutionOutlined,
  TrophyOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
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
}

function formatNow() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
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
      key: 'sale',
      label: '在线销售额',
      value: MONEY(summary.totalSaleAmount),
      tip: '各周期销售金额合计',
      icon: <ShoppingCartOutlined />,
      tone: 'wine',
      path: '/business/online-sale',
    },
    {
      key: 'payment',
      label: '回款金额',
      value: MONEY(summary.totalPaymentAmount),
      tip: '各周期回款金额合计',
      icon: <MoneyCollectOutlined />,
      tone: 'amber',
      path: '/business/online-sale',
    },
    {
      key: 'inventory',
      label: '库存金额',
      value: MONEY(summary.totalInventoryAmount),
      tip: `SKU ${summary.inventorySkuCount || 0} · 数量 ${MONEY(summary.totalInventoryQty)}`,
      icon: <DatabaseOutlined />,
      tone: 'slate',
      path: '/business/inventory',
    },
    {
      key: 'receivable',
      label: '应收账款',
      value: MONEY(summary.totalReceivableAmount),
      tip: `${summary.receivableCount || 0} 笔明细`,
      icon: <MoneyCollectOutlined />,
      tone: 'crimson',
      path: '/finance/receivable',
    },
    {
      key: 'customer',
      label: '客户开发额',
      value: MONEY(summary.totalCustomerDevAmount),
      tip: `${summary.customerDevCount || 0} 家客户`,
      icon: <SolutionOutlined />,
      tone: 'olive',
      path: '/business/customer-dev',
    },
    {
      key: 'dealer',
      label: '经销商排名',
      value: summary.dealerCount || 0,
      tip: '销售排名收录家数',
      icon: <TrophyOutlined />,
      tone: 'bronze',
      path: '/business/sales-rank',
      suffix: '家',
    },
  ]

  const trendOption = useMemo(() => {
    const list = data.onlineSaleTrend || []
    return {
      color: ['#8b1a1a', '#c9a227', '#4a6fa5'],
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '发货额', '回款额'], top: 0, textStyle: { color: '#595959' } },
      grid: { left: 48, right: 24, top: 40, bottom: 28 },
      xAxis: {
        type: 'category',
        data: list.map((i) => i.periodName || '-'),
        axisLine: { lineStyle: { color: '#d9d9d9' } },
        axisLabel: { color: '#8c8c8c' },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: '#8c8c8c',
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
      },
      series: [
        {
          name: '销售额',
          type: 'bar',
          barMaxWidth: 28,
          data: list.map((i) => Number(i.saleAmount || 0)),
          itemStyle: { borderRadius: [4, 4, 0, 0] },
        },
        {
          name: '发货额',
          type: 'line',
          smooth: true,
          data: list.map((i) => Number(i.shipAmount || 0)),
        },
        {
          name: '回款额',
          type: 'line',
          smooth: true,
          data: list.map((i) => Number(i.paymentAmount || 0)),
        },
      ],
    }
  }, [data.onlineSaleTrend])

  const structureOption = useMemo(() => {
    const list = data.productStructure || []
    return {
      color: ['#8b1a1a', '#c9a227', '#6b8f71', '#4a6fa5', '#a0522d', '#7a5c4e'],
      tooltip: {
        trigger: 'item',
        formatter: '{b}<br/>数量：{c}<br/>占比：{d}%',
      },
      legend: {
        orient: 'vertical',
        right: 8,
        top: 'middle',
        textStyle: { color: '#595959', fontSize: 12 },
      },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['38%', '52%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: list.map((i) => ({
            name: i.category,
            value: Number(i.quantity || i.ratio || 0),
          })),
        },
      ],
    }
  }, [data.productStructure])

  const rankOption = useMemo(() => {
    const list = [...(data.salesRank || [])].reverse()
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
      grid: { left: 100, right: 36, top: 16, bottom: 16 },
      xAxis: {
        type: 'value',
        axisLabel: {
          color: '#8c8c8c',
          formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
      },
      yAxis: {
        type: 'category',
        data: list.map((i) => i.companyName),
        axisLabel: { color: '#595959', width: 88, overflow: 'truncate' },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          type: 'bar',
          barMaxWidth: 18,
          data: list.map((i) => Number(i.amount || 0)),
          itemStyle: { borderRadius: [0, 6, 6, 0] },
          label: {
            show: true,
            position: 'right',
            color: '#8c8c8c',
            fontSize: 11,
            formatter: (p) => MONEY(p.value),
          },
        },
      ],
    }
  }, [data.salesRank])

  const inventoryOption = useMemo(() => {
    const list = data.inventory || []
    return {
      color: ['#4a6fa5', '#c9a227'],
      tooltip: { trigger: 'axis' },
      legend: { data: ['库存数量', '库存金额'], top: 0, textStyle: { color: '#595959' } },
      grid: { left: 48, right: 48, top: 40, bottom: 48 },
      xAxis: {
        type: 'category',
        data: list.map((i) => i.productName),
        axisLabel: { color: '#8c8c8c', interval: 0, rotate: list.length > 4 ? 20 : 0 },
      },
      yAxis: [
        {
          type: 'value',
          name: '数量',
          axisLabel: { color: '#8c8c8c' },
          splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
        },
        {
          type: 'value',
          name: '金额',
          axisLabel: {
            color: '#8c8c8c',
            formatter: (v) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : v),
          },
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: '库存数量',
          type: 'bar',
          barMaxWidth: 26,
          data: list.map((i) => Number(i.quantity || 0)),
          itemStyle: { borderRadius: [4, 4, 0, 0] },
        },
        {
          name: '库存金额',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          data: list.map((i) => Number(i.amount || 0)),
        },
      ],
    }
  }, [data.inventory])

  const receivableColumns = [
    {
      title: '客户',
      dataIndex: 'name',
      ellipsis: true,
    },
    {
      title: '金额',
      dataIndex: 'amount',
      width: 110,
      align: 'right',
      render: (v) => <span className={styles.money}>{MONEY(v)}</span>,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
      render: (v) => v || '-',
    },
  ]

  const customerColumns = [
    {
      title: '客户',
      dataIndex: 'name',
      ellipsis: true,
    },
    {
      title: '开发额',
      dataIndex: 'amount',
      width: 110,
      align: 'right',
      render: (v) => <span className={styles.money}>{MONEY(v)}</span>,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
      render: (v) => v || '-',
    },
  ]

  const rankColumns = [
    {
      title: '经销商',
      dataIndex: 'companyName',
      ellipsis: true,
    },
    {
      title: '销售额',
      dataIndex: 'amount',
      width: 100,
      align: 'right',
      render: (v) => MONEY(v),
    },
    {
      title: '占比',
      dataIndex: 'salesRatio',
      width: 72,
      align: 'right',
      render: (v) => `${Number(v || 0).toFixed(1)}%`,
    },
    {
      title: '趋势',
      dataIndex: 'trend',
      width: 64,
      align: 'center',
      render: (v) => {
        if (v > 0) return <Tag color="success" icon={<ArrowUpOutlined />}>升</Tag>
        if (v < 0) return <Tag color="error" icon={<ArrowDownOutlined />}>降</Tag>
        return <Tag>平</Tag>
      },
    },
  ]

  const chartH = fullscreen ? 360 : 280
  const chartHSm = fullscreen ? 340 : 260

  return (
    <div ref={pageRef} className={`${styles.page} ${fullscreen ? styles.fullscreen : ''}`}>
      <div className={styles.hero}>
        <div className={styles.heroText}>
          <div className={styles.eyebrow}>经营数据看板</div>
          <h1>{title || '汾源酒业经营体'}</h1>
          <p>
            你好，{name}。汇总业务管理与财务管理关键指标，点击卡片可进入对应明细。
          </p>
        </div>
        <div className={styles.heroMeta}>
          <span>数据更新：{updatedAt || '--'}</span>
          <Space size={8} className={styles.heroActions}>
            <Button
              type="primary"
              icon={fullscreen ? <CompressOutlined /> : <ExpandOutlined />}
              onClick={toggleFullscreen}
              className={styles.refreshBtn}
            >
              {fullscreen ? '退出全屏' : '全屏'}
            </Button>
            <Button
              type="primary"
              icon={<ReloadOutlined />}
              loading={loading}
              onClick={load}
              className={styles.refreshBtn}
            >
              刷新
            </Button>
          </Space>
        </div>
      </div>

      <Spin spinning={loading}>
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
                  <div className={styles.kpiLabel}>{card.label}</div>
                  <div className={styles.kpiValue}>
                    {card.value}
                    {card.suffix ? <small>{card.suffix}</small> : null}
                  </div>
                </div>
              </button>
            </Tooltip>
          ))}
        </div>

        <div className={styles.chartRow}>
          <section className={styles.panel}>
            <header className={styles.panelHead}>
              <h3>在线销售趋势</h3>
              <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/online-sale')}>
                明细
              </button>
            </header>
            {(data.onlineSaleTrend || []).length ? (
              <ReactECharts
                key={`trend-${chartKey}`}
                option={trendOption}
                style={{ height: chartH }}
                opts={{ renderer: 'canvas' }}
              />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无销售数据" />
            )}
          </section>

          <section className={styles.panel}>
            <header className={styles.panelHead}>
              <h3>销售产品结构</h3>
              <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/product-structure')}>
                明细
              </button>
            </header>
            {(data.productStructure || []).length ? (
              <ReactECharts
                key={`structure-${chartKey}`}
                option={structureOption}
                style={{ height: chartH }}
                opts={{ renderer: 'canvas' }}
              />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无产品结构数据" />
            )}
          </section>
        </div>

        <div className={styles.chartRow}>
          <section className={styles.panel}>
            <header className={styles.panelHead}>
              <h3>经销商销售排名</h3>
              <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/sales-rank')}>
                明细
              </button>
            </header>
            {(data.salesRank || []).length ? (
              <ReactECharts
                key={`rank-${chartKey}`}
                option={rankOption}
                style={{ height: chartHSm }}
                opts={{ renderer: 'canvas' }}
              />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无排名数据" />
            )}
          </section>

          <section className={styles.panel}>
            <header className={styles.panelHead}>
              <h3>汾源酒库存概况</h3>
              <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/inventory')}>
                明细
              </button>
            </header>
            {(data.inventory || []).length ? (
              <ReactECharts
                key={`inventory-${chartKey}`}
                option={inventoryOption}
                style={{ height: chartHSm }}
                opts={{ renderer: 'canvas' }}
              />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无库存数据" />
            )}
          </section>
        </div>

        <div className={styles.tableRow}>
          <section className={styles.panel}>
            <header className={styles.panelHead}>
              <h3>应收账款明细</h3>
              <button type="button" className={styles.linkBtn} onClick={() => navigate('/finance/receivable')}>
                明细
              </button>
            </header>
            <Table
              size="small"
              rowKey={(r) => `${r.name}-${r.amount}`}
              pagination={false}
              columns={receivableColumns}
              dataSource={data.receivable || []}
              locale={{ emptyText: '暂无应收数据' }}
            />
          </section>

          <section className={styles.panel}>
            <header className={styles.panelHead}>
              <h3>客户开发动态</h3>
              <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/customer-dev')}>
                明细
              </button>
            </header>
            <Table
              size="small"
              rowKey={(r) => `${r.name}-${r.amount}`}
              pagination={false}
              columns={customerColumns}
              dataSource={data.customerDev || []}
              locale={{ emptyText: '暂无客户开发数据' }}
            />
          </section>

          <section className={styles.panel}>
            <header className={styles.panelHead}>
              <h3>销售排名一览</h3>
              <button type="button" className={styles.linkBtn} onClick={() => navigate('/business/sales-rank')}>
                明细
              </button>
            </header>
            <Table
              size="small"
              rowKey={(r) => r.companyName}
              pagination={false}
              columns={rankColumns}
              dataSource={data.salesRank || []}
              locale={{ emptyText: '暂无排名数据' }}
            />
          </section>
        </div>
      </Spin>
    </div>
  )
}
