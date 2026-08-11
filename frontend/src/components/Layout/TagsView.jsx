import { Tag, Dropdown } from 'antd'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import {
  closeView,
  closeOthers,
  closeAll,
  closeLeft,
  closeRight,
  setActive,
} from '@/store/modules/tagsView'
import styles from './TagsView.module.css'

export default function TagsView() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { visitedViews, activePath } = useSelector((s) => s.tagsView)

  const go = (path) => {
    dispatch(setActive(path))
    navigate(path)
  }

  const contextMenu = (path) => ({
    items: [
      { key: 'refresh', label: '刷新', onClick: () => go(path) },
      { key: 'close', label: '关闭', onClick: () => onClose(path), disabled: path === '/dashboard' },
      { key: 'others', label: '关闭其他', onClick: () => { dispatch(closeOthers(path)); go(path) } },
      { key: 'left', label: '关闭左侧', onClick: () => dispatch(closeLeft(path)) },
      { key: 'right', label: '关闭右侧', onClick: () => dispatch(closeRight(path)) },
      {
        key: 'all',
        label: '关闭全部',
        onClick: () => {
          dispatch(closeAll())
          go('/dashboard')
        },
      },
    ],
  })

  const onClose = (path) => {
    const wasActive = activePath === path
    dispatch(closeView(path))
    if (wasActive) {
      // activePath 已在 reducer 中更新，用 store 下一 tick 不可靠，直接从 visited 推算
      const remaining = visitedViews.filter((v) => v.path !== path)
      const idx = visitedViews.findIndex((v) => v.path === path)
      const next = remaining[idx] || remaining[idx - 1] || { path: '/dashboard' }
      navigate(next.path)
    }
  }

  return (
    <div className={styles.tags}>
      {visitedViews.map((tag) => (
        <Dropdown key={tag.path} menu={contextMenu(tag.path)} trigger={['contextMenu']}>
          <Tag
            className={`${styles.tag} ${activePath === tag.path ? styles.active : ''}`}
            closable={tag.closable !== false}
            onClose={(e) => {
              e.preventDefault()
              onClose(tag.path)
            }}
            onClick={() => go(tag.path)}
          >
            {tag.title}
          </Tag>
        </Dropdown>
      ))}
    </div>
  )
}
