import { useEffect } from 'react'
import { Layout } from 'antd'
import { Outlet, useLocation } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import Header from './Header'
import Sidebar from './Sidebar'
import TagsView from './TagsView'
import BreadcrumbNav from './BreadcrumbNav'
import Footer from './Footer'
import { addView } from '@/store/modules/tagsView'
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
}

export default function MainLayout() {
  const location = useLocation()
  const dispatch = useDispatch()

  useEffect(() => {
    const title = ROUTE_TITLES[location.pathname] || location.pathname
    dispatch(addView({ path: location.pathname, title }))
  }, [location.pathname, dispatch])

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
