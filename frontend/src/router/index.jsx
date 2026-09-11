import { createBrowserRouter, Navigate } from 'react-router-dom'
import AuthGuard from './permission'
import MainLayout from '@/components/Layout'
import Login from '@/pages/Login'
import Dashboard from '@/pages/Dashboard'
import SubDashboard from '@/pages/SubDashboard'
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
import CustomerMaintainPage from '@/pages/Business/CustomerMaintain'
import PriceComparePage from '@/pages/Business/PriceCompare'
import OffsiteSalesPage from '@/pages/Business/OffsiteSales'
import CmCloudSyncPage from '@/pages/Business/CmCloudSync'
import ReceivablePage from '@/pages/Finance/Receivable'
import KingdeeVoucherPage from '@/pages/Finance/KingdeeVoucher'
import KingdeeCredentialPage from '@/pages/Kingdee/Credential'
import KingdeeAccountSetPage from '@/pages/Kingdee/AccountSet'
import BankVoucherPage from '@/pages/Kingdee/BankVoucher'
import SettingsPage from '@/pages/Settings'

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
        { path: 'sub-dashboard', element: <SubDashboard /> },
        { path: 'system/user', element: <UserPage /> },
        { path: 'system/role', element: <RolePage /> },
        { path: 'system/menu', element: <MenuPage /> },
        { path: 'system/dept', element: <DeptPage /> },
        { path: 'system/config', element: <ConfigPage /> },
        { path: 'settings/config', element: <ConfigPage /> },
        { path: 'log/oper', element: <OperLogPage /> },
        { path: 'log/login', element: <LoginLogPage /> },
        { path: 'business/customer-maintain', element: <CustomerMaintainPage /> },
        { path: 'business/price-compare', element: <PriceComparePage /> },
        { path: 'business/offsite-sales', element: <OffsiteSalesPage /> },
        { path: 'business/online-sale', element: <OnlineSalePage /> },
        { path: 'business/sales-rank', element: <SalesRankPage /> },
        { path: 'business/inventory', element: <InventoryPage /> },
        { path: 'business/product-structure', element: <ProductStructurePage /> },
        { path: 'business/customer-dev', element: <CustomerDevPage /> },
        { path: 'business/cmcloud-sync', element: <CmCloudSyncPage /> },
        { path: 'finance/receivable', element: <ReceivablePage /> },
        { path: 'finance/kingdee-voucher', element: <KingdeeVoucherPage /> },
        { path: 'kingdee/credential', element: <KingdeeCredentialPage /> },
        { path: 'kingdee/account-set', element: <KingdeeAccountSetPage /> },
        { path: 'kingdee/bank-voucher', element: <BankVoucherPage /> },
        { path: 'settings', element: <SettingsPage /> },
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
