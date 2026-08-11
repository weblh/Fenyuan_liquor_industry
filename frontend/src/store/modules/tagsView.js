import { createSlice } from '@reduxjs/toolkit'

const HOME_TAG = {
  title: '首页',
  path: '/dashboard',
  closable: false,
}

const tagsViewSlice = createSlice({
  name: 'tagsView',
  initialState: {
    visitedViews: [HOME_TAG],
    activePath: '/dashboard',
  },
  reducers: {
    addView(state, action) {
      const view = action.payload
      if (!view?.path) return
      state.activePath = view.path
      const exists = state.visitedViews.find((v) => v.path === view.path)
      if (!exists) {
        state.visitedViews.push({
          title: view.title || view.path,
          path: view.path,
          closable: view.path !== '/dashboard',
        })
      }
    },
    setActive(state, action) {
      state.activePath = action.payload
    },
    closeView(state, action) {
      const path = action.payload
      const idx = state.visitedViews.findIndex((v) => v.path === path)
      if (idx === -1) return
      const tag = state.visitedViews[idx]
      if (tag.closable === false) return
      state.visitedViews.splice(idx, 1)
      if (state.activePath === path) {
        const next = state.visitedViews[idx] || state.visitedViews[idx - 1] || HOME_TAG
        state.activePath = next.path
      }
    },
    closeOthers(state, action) {
      const path = action.payload || state.activePath
      state.visitedViews = state.visitedViews.filter(
        (v) => v.path === path || v.closable === false
      )
      state.activePath = path
    },
    closeAll(state) {
      state.visitedViews = [HOME_TAG]
      state.activePath = HOME_TAG.path
    },
    closeLeft(state, action) {
      const path = action.payload || state.activePath
      const idx = state.visitedViews.findIndex((v) => v.path === path)
      if (idx <= 0) return
      state.visitedViews = state.visitedViews.filter(
        (v, i) => i >= idx || v.closable === false
      )
    },
    closeRight(state, action) {
      const path = action.payload || state.activePath
      const idx = state.visitedViews.findIndex((v) => v.path === path)
      if (idx === -1) return
      state.visitedViews = state.visitedViews.filter(
        (v, i) => i <= idx || v.closable === false
      )
    },
  },
})

export const {
  addView,
  setActive,
  closeView,
  closeOthers,
  closeAll,
  closeLeft,
  closeRight,
} = tagsViewSlice.actions
export default tagsViewSlice.reducer
