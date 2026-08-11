import { Breadcrumb } from 'antd'
import { useLocation, Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { findMenuPath, menuTitle } from '@/utils/menu'
import styles from './BreadcrumbNav.module.css'

const TITLE_MAP = {
  '/dashboard': '首页',
  '/system/user': '用户管理',
  '/system/role': '角色管理',
  '/system/menu': '菜单管理',
  '/system/dept': '部门管理',
  '/system/config': '参数配置',
  '/settings/config': '参数配置',
  '/log/oper': '操作日志',
  '/log/login': '登录日志',
}

export default function BreadcrumbNav() {
  const location = useLocation()
  const menus = useSelector((s) => s.user.menus)
  const trail = findMenuPath(menus || [], location.pathname)

  const items =
    trail.length > 0
      ? [
          { title: <Link to="/dashboard">首页</Link> },
          ...trail.map((m, i) => ({
            title: i === trail.length - 1 ? menuTitle(m) : menuTitle(m),
          })),
        ]
      : [
          { title: <Link to="/dashboard">首页</Link> },
          ...(location.pathname !== '/dashboard'
            ? [{ title: TITLE_MAP[location.pathname] || location.pathname }]
            : []),
        ]

  return (
    <div className={styles.wrap}>
      <Breadcrumb items={items} />
    </div>
  )
}
