import { configureStore } from '@reduxjs/toolkit'
import userReducer from './modules/user'
import appReducer from './modules/app'
import permissionReducer from './modules/permission'
import tagsViewReducer from './modules/tagsView'

const store = configureStore({
  reducer: {
    user: userReducer,
    app: appReducer,
    permission: permissionReducer,
    tagsView: tagsViewReducer,
  },
})

export default store
