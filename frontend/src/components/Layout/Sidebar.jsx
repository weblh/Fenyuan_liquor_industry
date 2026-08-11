import { useMemo } from 'react'
import { Layout, Menu } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import { useSelector } from 'react-redux'
import * as Icons from '@ant-design/icons'
import { filterVisibleMenus, menuTitle } from '@/utils/menu'
import styles from './Sidebar.module.css'

const { Sider } = Layout

function resolveIcon(iconName) {
  if (!iconName) return null
  const Comp = Icons[iconName]
  return Comp ? <Comp /> : null
}

function mapMenusToItems(menus = []) {
  return menus.map((m) => {
    const path = m.path?.startsWith('/') ? m.path : m.path ? `/${m.path}` : undefined
    const item = {
      key: path || String(m.id),
      icon: resolveIcon(m.icon),
      label: menuTitle(m),
    }
    if (m.children?.length) {
      item.children = mapMenusToItems(m.children)
    }
    return item
  })
}

const DASHBOARD_ITEM = {
  id: 0,
  path: '/dashboard',
  name: '首页',
  icon: 'DashboardOutlined',
  type: 1,
  visible: 1,
  status: 1,
}

const FALLBACK_MENUS = [
  DASHBOARD_ITEM,
  {
    id: 1,
    path: '/system',
    name: '系统管理',
    icon: 'SettingOutlined',
    children: [
      { id: 21, path: '/system/user', name: '用户管理', icon: 'UserOutlined' },
      { id: 22, path: '/system/role', name: '角色管理', icon: 'TeamOutlined' },
      { id: 23, path: '/system/menu', name: '菜单管理', icon: 'MenuOutlined' },
      { id: 24, path: '/system/dept', name: '部门管理', icon: 'ApartmentOutlined' },
    ],
  },
  {
    id: 3,
    path: '/log',
    name: '日志管理',
    icon: 'FileTextOutlined',
    children: [
      { id: 31, path: '/log/oper', name: '操作日志', icon: 'ProfileOutlined' },
      { id: 32, path: '/log/login', name: '登录日志', icon: 'LoginOutlined' },
    ],
  },
  {
    id: 9,
    path: '/settings',
    name: '系统配置',
    icon: 'ToolOutlined',
    children: [
      { id: 25, path: '/settings/config', name: '参数配置', icon: 'SettingOutlined' },
    ],
  },
  {
    id: 26,
    path: '/business',
    name: '业务管理',
    icon: 'ShopOutlined',
    children: [
      { id: 27, path: '/business/online-sale', name: '在线销售管理', icon: 'ShoppingCartOutlined' },
      { id: 28, path: '/business/sales-rank', name: '销售排名', icon: 'TrophyOutlined' },
      { id: 29, path: '/business/inventory', name: '汾源酒库存', icon: 'DatabaseOutlined' },
      { id: 30, path: '/business/product-structure', name: '销售产品结构', icon: 'PieChartOutlined' },
      { id: 31, path: '/business/customer-dev', name: '客户开发', icon: 'SolutionOutlined' },
    ],
  },
  {
    id: 32,
    path: '/finance',
    name: '财务管理',
    icon: 'AccountBookOutlined',
    children: [
      { id: 33, path: '/finance/receivable', name: '应收账款明细', icon: 'MoneyCollectOutlined' },
    ],
  },
]

export default function Sidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const collapsed = useSelector((s) => s.app.collapsed)
  const title = useSelector((s) => s.app.title)
  const menus = useSelector((s) => s.user.menus)

  const items = useMemo(() => {
    const source = menus?.length ? filterVisibleMenus(menus) : FALLBACK_MENUS
    const withHome = source.some((m) => m.path === '/dashboard')
      ? source
      : [DASHBOARD_ITEM, ...source]
    return mapMenusToItems(withHome)
  }, [menus])

  const selectedKeys = [location.pathname]
  const openKeys = useMemo(() => {
    const parts = location.pathname.split('/').filter(Boolean)
    if (parts.length <= 1) return []
    return [`/${parts[0]}`]
  }, [location.pathname])

  return (
    <Sider
      collapsible
      collapsed={collapsed}
      trigger={null}
      width={220}
      className={styles.sider}
      theme="dark"
    >
      <div className={styles.logo}>
        <span className={styles.logoMark}>汾</span>
        {!collapsed && <span className={styles.logoText}>{title}</span>}
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={selectedKeys}
        defaultOpenKeys={openKeys}
        items={items}
        onClick={({ key }) => {
          if (key.startsWith('/')) navigate(key)
        }}
      />
    </Sider>
  )
}
