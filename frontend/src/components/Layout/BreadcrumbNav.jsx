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
  '/business/customer-maintain': '客户维护监管',
  '/business/price-compare': '酒类价格对比',
  '/business/offsite-sales': '异地销售统计',
  '/business/online-sale': '在线销售管理',
  '/business/sales-rank': '销售排名',
  '/business/inventory': '汾源酒库存',
  '/business/product-structure': '销售产品结构',
  '/business/customer-dev': '客户开发',
  '/business/cmcloud-sync': '管家婆同步',
  '/finance/kingdee-voucher': '凭证记录',
  '/kingdee/credential': '账号密码',
  '/kingdee/account-set': '账套',
  '/kingdee/bank-voucher': '银行流水凭证',
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
