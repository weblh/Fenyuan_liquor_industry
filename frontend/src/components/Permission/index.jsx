import usePermission from '@/hooks/usePermission'

/**
 * 按钮级权限：无权限时不渲染 children
 * @param {{ permission?: string | string[], role?: string | string[], children: React.ReactNode }} props
 */
export default function Permission({ permission, role, children, fallback = null }) {
  const { hasPerm, hasRole } = usePermission()

  if (permission && !hasPerm(permission)) {
    return fallback
  }
  if (role && !hasRole(role)) {
    return fallback
  }
  return children
}
