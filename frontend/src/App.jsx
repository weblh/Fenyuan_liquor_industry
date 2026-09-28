import { RouterProvider } from 'react-router-dom'
import { ConfigProvider, App as AntApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import router from '@/router'

const theme = {
  token: {
    colorPrimary: '#2f80ed',
    borderRadius: 6,
    // 深色蓝黑主题
    colorText: '#e8f0fc',
    colorTextSecondary: '#9db2d0',
    colorTextTertiary: '#7d92b5',
    colorTextQuaternary: '#4a6080',
    colorBgContainer: '#13233c',
    colorBgElevated: '#11223e',
    colorBgLayout: '#0b1729',
    colorBgSpotlight: '#11223e',
    colorBorder: 'rgba(90, 165, 255, 0.18)',
    colorBorderSecondary: 'rgba(90, 165, 255, 0.1)',
    colorSplit: 'rgba(90, 165, 255, 0.1)',
    colorBgMask: 'rgba(7, 14, 26, 0.75)',
    colorTextLightSolid: '#0b1729',
    colorFill: 'rgba(47, 128, 237, 0.12)',
    colorFillSecondary: 'rgba(47, 128, 237, 0.08)',
    colorFillTertiary: 'rgba(20, 37, 66, 0.6)',
    colorFillQuaternary: 'rgba(20, 37, 66, 0.4)',
    colorPrimaryHover: '#4a9fff',
    colorPrimaryActive: '#2570d8',
  },
  components: {
    Layout: {
      bodyBg: '#0b1729',
      headerBg: '#0d1a30',
      siderBg: '#0b1729',
    },
    Menu: {
      darkItemBg: '#0b1729',
      darkSubMenuItemBg: '#070e1a',
      darkItemSelectedBg: 'rgba(47, 128, 237, 0.22)',
      darkItemHoverBg: 'rgba(47, 128, 237, 0.12)',
      darkItemColor: '#9db2d0',
      darkItemHoverColor: '#e8f0fc',
      darkItemSelectedColor: '#7cb8ff',
    },
    Table: {
      headerBg: '#162849',
      headerColor: '#9db2d0',
      rowHoverBg: 'rgba(47, 128, 237, 0.07)',
      borderColor: 'rgba(90, 165, 255, 0.08)',
    },
    Modal: {
      contentBg: '#13233c',
      headerBg: 'transparent',
    },
    Card: {
      colorBgContainer: 'transparent',
    },
    Input: {
      colorBgContainer: 'rgba(7, 14, 26, 0.55)',
      activeBorderColor: '#2f80ed',
      hoverBorderColor: 'rgba(90, 165, 255, 0.45)',
    },
    Select: {
      colorBgContainer: 'rgba(7, 14, 26, 0.55)',
      optionSelectedBg: 'rgba(47, 128, 237, 0.26)',
    },
    Pagination: {
      itemBg: 'rgba(20, 37, 66, 0.6)',
      itemActiveBg: '#2f80ed',
    },
    Tree: {
      nodeHoverBg: 'rgba(47, 128, 237, 0.14)',
      nodeSelectedBg: 'rgba(47, 128, 237, 0.26)',
    },
    DatePicker: {
      colorBgContainer: 'rgba(7, 14, 26, 0.55)',
    },
  },
}

export default function App() {
  return (
    <ConfigProvider locale={zhCN} theme={theme}>
      <AntApp>
        <RouterProvider router={router} />
      </AntApp>
    </ConfigProvider>
  )
}
