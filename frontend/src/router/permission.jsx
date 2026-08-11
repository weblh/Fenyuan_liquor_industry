import { useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Spin } from 'antd'
import { fetchUserInfo, clearUser } from '@/store/modules/user'
import { setMenus, setPermissions } from '@/store/modules/permission'
import { getToken } from '@/utils/storage'

/**
 * 路由守卫：无 token 跳登录；有 token 拉取用户信息与菜单
 */
export default function AuthGuard({ children }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const token = useSelector((s) => s.user.token) || getToken()
  const userInfo = useSelector((s) => s.user.userInfo)
  const menus = useSelector((s) => s.user.menus)
  const permissions = useSelector((s) => s.user.permissions)
  const [ready, setReady] = useState(!!userInfo)

  useEffect(() => {
    if (!token) {
      setReady(false)
      return
    }
    if (userInfo) {
      dispatch(setMenus(menus))
      dispatch(setPermissions(permissions))
      setReady(true)
      return
    }
    let cancelled = false
    ;(async () => {
      try {
        const result = await dispatch(fetchUserInfo()).unwrap()
        if (!cancelled) {
          dispatch(setMenus(result.menus))
          dispatch(setPermissions(result.permissions))
          setReady(true)
        }
      } catch {
        if (!cancelled) {
          dispatch(clearUser())
          navigate(`/login?redirect=${encodeURIComponent(location.pathname)}`, { replace: true })
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [token])

  if (!token) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />
  }

  if (!ready) {
    return (
      <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Spin size="large" tip="加载用户信息..." />
      </div>
    )
  }

  return children
}
