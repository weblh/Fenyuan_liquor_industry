import { RouterProvider } from 'react-router-dom'
import { ConfigProvider, App as AntApp, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import router from '@/router'

const themeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    // 蓝黑主题
    colorPrimary: '#2f7cf6',
    colorInfo: '#2f7cf6',
    borderRadius: 6,
    // 背景层次：黑蓝底 → 容器 → 浮层
    colorBgBase: '#080d18',
    colorBgLayout: '#080d18',
    colorBgContainer: '#0f1830',
    colorBgElevated: '#152238',
    colorBgSpotlight: '#1a2942',
    // 文字
    colorText: '#dce7f5',
    colorTextSecondary: '#9fb2cc',
    colorTextTertiary: '#6f83a0',
    colorTextQuaternary: '#4e607a',
    // 边框/分隔线
    colorBorder: '#223355',
    colorBorderSecondary: '#1a2740',
    colorSplit: 'rgba(80, 120, 180, 0.16)',
    // 填充
    colorFill: 'rgba(90, 140, 210, 0.16)',
    colorFillSecondary: 'rgba(90, 140, 210, 0.12)',
    colorFillTertiary: 'rgba(90, 140, 210, 0.08)',
    colorFillQuaternary: 'rgba(90, 140, 210, 0.05)',
  },
  components: {
    Layout: {
      headerBg: '#0c1426',
      siderBg: '#0c1426',
      bodyBg: '#080d18',
      headerHeight: 56,
      headerPadding: '0 16px',
    },
    Menu: {
      darkItemBg: '#0c1426',
      darkSubMenuItemBg: '#080f1e',
      darkItemSelectedBg: 'rgba(47, 124, 246, 0.22)',
      darkItemHoverBg: 'rgba(47, 124, 246, 0.12)',
      darkItemColor: '#9fb2cc',
      darkItemSelectedColor: '#7db4ff',
      darkItemHoverColor: '#cfe2ff',
    },
    Table: {
      headerBg: '#13203a',
      headerColor: '#9fb2cc',
      rowHoverBg: 'rgba(47, 124, 246, 0.08)',
      borderColor: '#1a2740',
      headerSplitColor: '#1a2740',
    },
    Card: {
      colorBgContainer: '#0f1830',
      colorBorderSecondary: '#1a2740',
    },
    Modal: {
      contentBg: '#0f1830',
      headerBg: '#0f1830',
    },
    Input: {
      colorBgContainer: 'rgba(10, 18, 36, 0.6)',
      activeBorderColor: '#2f7cf6',
      hoverBorderColor: '#4d8ff8',
    },
    Select: {
      colorBgContainer: 'rgba(10, 18, 36, 0.6)',
      optionSelectedBg: 'rgba(47, 124, 246, 0.22)',
    },
    DatePicker: {
      colorBgContainer: 'rgba(10, 18, 36, 0.6)',
    },
    Button: {
      primaryShadow: 'none',
    },
    Tabs: {
      itemColor: '#9fb2cc',
      itemSelectedColor: '#4d8ff8',
      inkBarColor: '#2f7cf6',
    },
    Pagination: {
      itemBg: 'transparent',
    },
    Dropdown: {
      controlItemBgHover: 'rgba(47, 124, 246, 0.18)',
    },
    Tooltip: {
      colorBgSpotlight: '#152238',
    },
  },
}

export default function App() {
  return (
    <ConfigProvider locale={zhCN} theme={themeConfig}>
      <AntApp>
        <RouterProvider router={router} />
      </AntApp>
    </ConfigProvider>
  )
}
