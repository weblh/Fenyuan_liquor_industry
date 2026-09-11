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

const SUB_DASHBOARD_ITEM = {
  id: 90,
  path: '/sub-dashboard',
  name: '附页',
  icon: 'AppstoreOutlined',
  type: 1,
  visible: 1,
  status: 1,
}

const FALLBACK_MENUS = [
  DASHBOARD_ITEM,
  {
    id: 26,
    path: '/business',
    name: '业务管理',
    icon: 'ShopOutlined',
    children: [
      { id: 52, path: '/business/customer-maintain', name: '客户维护监管', icon: 'CustomerServiceOutlined' },
      { id: 56, path: '/business/price-compare', name: '酒类价格对比', icon: 'FundOutlined' },
      { id: 60, path: '/business/offsite-sales', name: '异地销售统计', icon: 'EnvironmentOutlined' },
      { id: 27, path: '/business/online-sale', name: '在线销售管理', icon: 'ShoppingCartOutlined' },
      { id: 28, path: '/business/sales-rank', name: '销售排名', icon: 'TrophyOutlined' },
      { id: 29, path: '/business/inventory', name: '汾源酒库存', icon: 'DatabaseOutlined' },
      { id: 30, path: '/business/product-structure', name: '销售产品结构', icon: 'PieChartOutlined' },
      { id: 31, path: '/business/customer-dev', name: '客户开发', icon: 'SolutionOutlined' },
      { id: 80, path: '/business/cmcloud-sync', name: '管家婆同步', icon: 'CloudSyncOutlined' },
    ],
  },
  {
    id: 32,
    path: '/finance',
    name: '财务管理',
    icon: 'AccountBookOutlined',
    children: [
      { id: 64, path: '/finance/kingdee-voucher', name: '凭证记录', icon: 'FileSyncOutlined' },
    ],
  },
  {
    id: 68,
    path: '/kingdee',
    name: '金蝶',
    icon: 'CloudOutlined',
    children: [
      { id: 69, path: '/kingdee/credential', name: '账号密码', icon: 'KeyOutlined' },
      { id: 70, path: '/kingdee/account-set', name: '账套', icon: 'DatabaseOutlined' },
      { id: 77, path: '/kingdee/bank-voucher', name: '银行流水凭证', icon: 'BankOutlined' },
    ],
  },
  {
    id: 90,
    path: '/sub-dashboard',
    name: '附页',
    icon: 'AppstoreOutlined',
    type: 1,
    visible: 1,
    status: 1,
  },
  {
    id: 99,
    path: '/settings',
    name: '设置',
    icon: 'SettingOutlined',
    children: [
      {
        id: 1,
        path: '/system',
        name: '系统管理',
        icon: 'UserSwitchOutlined',
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
      { id: 25, path: '/settings/config', name: '参数配置', icon: 'ControlOutlined' },
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
    // 从后端菜单里提取要并入"设置"的三组，然后从顶层删掉
    const moveToSettings = ['系统管理', '日志管理', '系统配置']
    let collected = []
    let stripped = source.filter((m) => {
      if (moveToSettings.includes(m.name)) {
        collected.push(m)
        return false
      }
      return true
    })
    // 系统配置的 path 也是 /settings，可能单独匹配
    stripped = stripped.filter((m) => !(m.path === '/settings' && m.name !== '设置'))
    const withHome = stripped.some((m) => m.path === '/dashboard')
      ? stripped
      : [DASHBOARD_ITEM, ...stripped]
    const withSub = withHome.some((m) => m.path === '/sub-dashboard')
      ? withHome
      : [...withHome, SUB_DASHBOARD_ITEM]
    // 构造/合并"设置"组
    const SETTINGS_CHILDREN_ORDER = ['系统管理', '日志管理', '参数配置']
    const childrenMap = {}
    collected.forEach((m) => {
      // 系统配置 → 参数配置（把它变成设置的子项）
      if (m.name === '系统配置') {
        ;(m.children || []).forEach((c) => {
          if (c.name === '参数配置') childrenMap['参数配置'] = c
        })
      } else {
        childrenMap[m.name] = m
      }
    })
    const settingsChildren = SETTINGS_CHILDREN_ORDER
      .map((name) => childrenMap[name])
      .filter(Boolean)

    const existingSettings = withSub.find((m) => m.name === '设置')
    if (existingSettings) {
      const existingPaths = new Set((existingSettings.children || []).map((c) => c.path))
      settingsChildren.forEach((c) => {
        if (c.path && !existingPaths.has(c.path)) {
          existingSettings.children = existingSettings.children || []
          existingSettings.children.push(c)
        }
      })
      return mapMenusToItems(withSub)
    }
    const SETTINGS_ITEM = {
      id: 99,
      path: '/settings',
      name: '设置',
      icon: 'SettingOutlined',
      children:
        settingsChildren.length > 0
          ? settingsChildren
          : [
              {
                id: 1,
                path: '/system',
                name: '系统管理',
                icon: 'UserSwitchOutlined',
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
              { id: 25, path: '/settings/config', name: '参数配置', icon: 'ControlOutlined' },
            ],
    }
    return mapMenusToItems([...withSub, SETTINGS_ITEM])
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
