/**
 * 判断是否拥有指定权限（*:*:* 表示全部）
 */
export function hasPermission(permissions = [], permission) {
  if (!permission) return true
  if (!permissions?.length) return false
  if (permissions.includes('*:*:*') || permissions.includes('*')) return true
  if (Array.isArray(permission)) {
    return permission.some((p) => permissions.includes(p))
  }
  return permissions.includes(permission)
}

/**
 * 判断是否拥有任一角色
 */
export function hasRole(roles = [], role) {
  if (!role) return true
  if (!roles?.length) return false
  if (roles.includes('admin') || roles.includes('ROLE_ADMIN')) return true
  if (Array.isArray(role)) {
    return role.some((r) => roles.includes(r))
  }
  return roles.includes(role)
}
