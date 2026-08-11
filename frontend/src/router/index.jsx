import { createBrowserRouter, Navigate } from 'react-router-dom'
import AuthGuard from './permission'
import MainLayout from '@/components/Layout'
import Login from '@/pages/Login'
import Dashboard from '@/pages/Dashboard'
import UserPage from '@/pages/System/User'
import RolePage from '@/pages/System/Role'
import MenuPage from '@/pages/System/Menu'
import DeptPage from '@/pages/System/Dept'
import ConfigPage from '@/pages/System/Config'
import OperLogPage from '@/pages/Log/Oper'
import LoginLogPage from '@/pages/Log/Login'
import OnlineSalePage from '@/pages/Business/OnlineSale'
import SalesRankPage from '@/pages/Business/SalesRank'
import InventoryPage from '@/pages/Business/Inventory'
import ProductStructurePage from '@/pages/Business/ProductStructure'
import CustomerDevPage from '@/pages/Business/CustomerDev'
import ReceivablePage from '@/pages/Finance/Receivable'

const basename = import.meta.env.BASE_URL.replace(/\/$/, '') || undefined

const router = createBrowserRouter(
  [
    {
      path: '/login',
      element: <Login />,
    },
    {
      path: '/',
      element: (
        <AuthGuard>
          <MainLayout />
        </AuthGuard>
      ),
      children: [
        { index: true, element: <Navigate to="/dashboard" replace /> },
        { path: 'dashboard', element: <Dashboard /> },
        { path: 'system/user', element: <UserPage /> },
        { path: 'system/role', element: <RolePage /> },
        { path: 'system/menu', element: <MenuPage /> },
        { path: 'system/dept', element: <DeptPage /> },
        { path: 'system/config', element: <ConfigPage /> },
        { path: 'settings/config', element: <ConfigPage /> },
        { path: 'log/oper', element: <OperLogPage /> },
        { path: 'log/login', element: <LoginLogPage /> },
        { path: 'business/online-sale', element: <OnlineSalePage /> },
        { path: 'business/sales-rank', element: <SalesRankPage /> },
        { path: 'business/inventory', element: <InventoryPage /> },
        { path: 'business/product-structure', element: <ProductStructurePage /> },
        { path: 'business/customer-dev', element: <CustomerDevPage /> },
        { path: 'finance/receivable', element: <ReceivablePage /> },
      ],
    },
    {
      path: '*',
      element: <Navigate to="/dashboard" replace />,
    },
  ],
  { basename },
)

export default router
