import { createSlice } from '@reduxjs/toolkit'

const permissionSlice = createSlice({
  name: 'permission',
  initialState: {
    routes: [],
    menus: [],
    permissions: [],
  },
  reducers: {
    setRoutes(state, action) {
      state.routes = action.payload
    },
    setMenus(state, action) {
      state.menus = action.payload
    },
    setPermissions(state, action) {
      state.permissions = action.payload
    },
    resetPermission(state) {
      state.routes = []
      state.menus = []
      state.permissions = []
    },
  },
})

export const { setRoutes, setMenus, setPermissions, resetPermission } = permissionSlice.actions
export default permissionSlice.reducer
