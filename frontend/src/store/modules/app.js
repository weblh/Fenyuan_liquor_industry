import { createSlice } from '@reduxjs/toolkit'

const appSlice = createSlice({
  name: 'app',
  initialState: {
    collapsed: false,
    title: '汾源酒业经营体',
  },
  reducers: {
    toggleCollapsed(state) {
      state.collapsed = !state.collapsed
    },
    setCollapsed(state, action) {
      state.collapsed = action.payload
    },
  },
})

export const { toggleCollapsed, setCollapsed } = appSlice.actions
export default appSlice.reducer
