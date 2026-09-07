import { useEffect } from 'react'
import { Layout } from 'antd'
import { Outlet, useLocation } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import Header from './Header'
import Sidebar from './Sidebar'
import TagsView from './TagsView'
import BreadcrumbNav from './BreadcrumbNav'
import Footer from './Footer'
import { addView } from '@/store/modules/tagsView'
import { findMenuPath, menuTitle } from '@/utils/menu'
import styles from './index.module.css'

const { Content } = Layout

const ROUTE_TITLES = {
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

export default function MainLayout() {
  const location = useLocation()
  const dispatch = useDispatch()
  const menus = useSelector((s) => s.user.menus)

  useEffect(() => {
    const trail = findMenuPath(menus || [], location.pathname)
    const fromMenu = trail.length ? menuTitle(trail[trail.length - 1]) : null
    const title = fromMenu || ROUTE_TITLES[location.pathname] || location.pathname
    dispatch(addView({ path: location.pathname, title }))
  }, [location.pathname, dispatch, menus])

  return (
    <Layout className={styles.root}>
      <Sidebar />
      <Layout className={styles.main}>
        <Header />
        <TagsView />
        <BreadcrumbNav />
        <Content className={styles.content}>
          <div className={location.pathname === '/dashboard' ? styles.panelFlush : styles.panel}>
            <Outlet />
          </div>
        </Content>
        <Footer />
      </Layout>
    </Layout>
  )
}
