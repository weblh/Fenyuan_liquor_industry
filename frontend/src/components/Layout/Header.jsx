import { Layout, Dropdown, Space, Avatar, theme } from 'antd'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { toggleCollapsed } from '@/store/modules/app'
import { logout } from '@/store/modules/user'
import { resetPermission } from '@/store/modules/permission'
import styles from './Header.module.css'

const { Header: AntHeader } = Layout

export default function Header() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const collapsed = useSelector((s) => s.app.collapsed)
  const title = useSelector((s) => s.app.title)
  const userInfo = useSelector((s) => s.user.userInfo)
  const { token } = theme.useToken()

  const onLogout = async () => {
    await dispatch(logout())
    dispatch(resetPermission())
    navigate('/login', { replace: true })
  }

  const items = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: onLogout,
    },
  ]

  const displayName = userInfo?.nickname || userInfo?.username || userInfo?.userName || '用户'

  return (
    <AntHeader
      className={styles.header}
      style={{ background: token.colorBgContainer }}
    >
      <div className={styles.left}>
        <span className={styles.trigger} onClick={() => dispatch(toggleCollapsed())}>
          {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        </span>
        <span className={styles.title}>{title}</span>
      </div>
      <Dropdown menu={{ items }} placement="bottomRight">
        <Space className={styles.user} size={8}>
          <Avatar size="small" icon={<UserOutlined />} src={userInfo?.avatar} />
          <span>{displayName}</span>
        </Space>
      </Dropdown>
    </AntHeader>
  )
}
