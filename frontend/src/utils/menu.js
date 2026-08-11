/**
 * 将扁平菜单转为树结构
 */
export function listToTree(list = [], idKey = 'id', parentKey = 'parentId', childrenKey = 'children') {
  const map = {}
  const roots = []
  list.forEach((item) => {
    map[item[idKey]] = { ...item, [childrenKey]: [] }
  })
  list.forEach((item) => {
    const node = map[item[idKey]]
    const parentId = item[parentKey]
    if (parentId != null && parentId !== 0 && map[parentId]) {
      map[parentId][childrenKey].push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/**
 * 侧边栏可见菜单：排除按钮(type=2)、隐藏(visible=0)、停用(status=0)
 */
export function filterVisibleMenus(menus = []) {
  return menus
    .filter((m) => {
      if (m.type === 2 || m.type === '2') return false
      if (m.visible === 0 || m.visible === '0' || m.visible === false) return false
      if (m.status === 0 || m.status === '0') return false
      return true
    })
    .map((m) => ({
      ...m,
      children: m.children?.length ? filterVisibleMenus(m.children) : undefined,
    }))
}

/**
 * 按 path 在菜单树中查找面包屑路径
 */
export function findMenuPath(menus = [], pathname) {
  const result = []
  const walk = (nodes, trail) => {
    for (const node of nodes) {
      const next = [...trail, node]
      const p = node.path || ''
      if (p === pathname || `/${p}`.replace(/\/+/g, '/') === pathname) {
        result.push(...next)
        return true
      }
      if (node.children?.length && walk(node.children, next)) {
        return true
      }
    }
    return false
  }
  walk(menus, [])
  return result
}

/**
 * 从菜单树提取权限标识
 */
export function extractPermissions(menus = []) {
  const set = new Set()
  const walk = (nodes) => {
    nodes.forEach((n) => {
      if (n.permission) set.add(n.permission)
      if (n.perms) set.add(n.perms)
      if (n.children?.length) walk(n.children)
    })
  }
  walk(menus)
  return [...set]
}

/** 菜单显示名 */
export function menuTitle(m) {
  return m?.meta?.title || m?.title || m?.menuName || m?.name || ''
}
