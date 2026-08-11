import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { getInfo, login as loginApi, logout as logoutApi, getCaptcha } from '@/api/modules/auth'
import { getUserMenus } from '@/api/modules/menu'
import { getToken, setToken, removeToken, clearAuth } from '@/utils/storage'
import { listToTree, extractPermissions } from '@/utils/menu'

export const fetchCaptcha = createAsyncThunk('user/fetchCaptcha', async () => {
  return await getCaptcha()
})

export const login = createAsyncThunk('user/login', async (payload) => {
  const data = await loginApi({
    username: payload.username,
    password: payload.password,
    captcha: payload.captcha,
    captchaKey: payload.captchaKey,
  })
  const token = data?.token || data?.accessToken
  if (token) setToken(token)
  return data
})

export const fetchUserInfo = createAsyncThunk('user/fetchUserInfo', async () => {
  const info = await getInfo()
  let menus = info?.menus
  if (!menus?.length) {
    try {
      menus = await getUserMenus()
    } catch {
      menus = []
    }
  }
  if (Array.isArray(menus) && menus.length && !menus.some((m) => m.children?.length)) {
    const hasParent = menus.some((m) => m.parentId != null)
    if (hasParent) menus = listToTree(menus)
  }
  const permissions =
    info?.permissions ||
    info?.perms ||
    extractPermissions(menus || [])
  const permissionList = Array.isArray(permissions)
    ? permissions
    : permissions
      ? [...permissions]
      : []
  return {
    userInfo: info?.user || info?.userInfo || info,
    menus: menus || [],
    permissions: permissionList,
    roles: info?.roles ? (Array.isArray(info.roles) ? info.roles : [...info.roles]) : [],
  }
})

export const logout = createAsyncThunk('user/logout', async () => {
  try {
    await logoutApi()
  } catch {
    // ignore
  } finally {
    clearAuth()
  }
})

const userSlice = createSlice({
  name: 'user',
  initialState: {
    token: getToken() || '',
    userInfo: null,
    permissions: [],
    roles: [],
    menus: [],
    captcha: null,
    loading: false,
  },
  reducers: {
    setTokenState(state, action) {
      state.token = action.payload
      if (action.payload) setToken(action.payload)
      else removeToken()
    },
    clearUser(state) {
      state.token = ''
      state.userInfo = null
      state.permissions = []
      state.roles = []
      state.menus = []
      clearAuth()
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCaptcha.fulfilled, (state, action) => {
        state.captcha = action.payload
      })
      .addCase(login.fulfilled, (state, action) => {
        state.token = action.payload?.token || action.payload?.accessToken || state.token
        if (action.payload?.userInfo) {
          state.userInfo = action.payload.userInfo
        }
      })
      .addCase(fetchUserInfo.pending, (state) => {
        state.loading = true
      })
      .addCase(fetchUserInfo.fulfilled, (state, action) => {
        state.loading = false
        state.userInfo = action.payload.userInfo
        state.menus = action.payload.menus
        state.permissions = action.payload.permissions
        state.roles = action.payload.roles
      })
      .addCase(fetchUserInfo.rejected, (state) => {
        state.loading = false
      })
      .addCase(logout.fulfilled, (state) => {
        state.token = ''
        state.userInfo = null
        state.permissions = []
        state.roles = []
        state.menus = []
      })
  },
})

export const { setTokenState, clearUser } = userSlice.actions
export default userSlice.reducer
