import { useSelector } from 'react-redux'
import { hasPermission, hasRole } from '@/utils/permission'

export default function usePermission() {
  const permissions = useSelector((s) => s.user.permissions)
  const roles = useSelector((s) => s.user.roles)

  return {
    permissions,
    roles,
    hasPerm: (perm) => hasPermission(permissions, perm),
    hasRole: (role) => hasRole(roles, role),
  }
}
