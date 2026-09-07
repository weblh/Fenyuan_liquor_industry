import { useEffect, useState } from 'react'
import { Alert, Button, Card, Space, Typography, Upload, message, Tag } from 'antd'
import { CloudSyncOutlined, DatabaseOutlined, FileExcelOutlined, ShoppingOutlined } from '@ant-design/icons'
import { cmcloudSyncApi } from '@/api/modules/business'
import request from '@/utils/request'

const { Paragraph, Text } = Typography

/**
 * 管家婆云财贸（CMCloud）→ 本平台本地库。
 * 正式路径：登录 CMCloud 后一键同步写入 MySQL；Excel 仅为 API 未开通时的兜底。
 */
export default function CmCloudSyncPage() {
  const [syncingMaster, setSyncingMaster] = useState(false)
  const [syncingBills, setSyncingBills] = useState(false)
  const [importingSales, setImportingSales] = useState(false)
  const [importingInv, setImportingInv] = useState(false)
  const [checking, setChecking] = useState(false)
  const [apiOk, setApiOk] = useState(null)
  const [lastMasterTip, setLastMasterTip] = useState('')
  const [lastBillTip, setLastBillTip] = useState('')
  const [lastImportTip, setLastImportTip] = useState('')

  const checkApi = async () => {
    setChecking(true)
    try {
      // 轻量探测：走后端 sync-bills 会很重；用公开 captcha 同机探测改由前端直连易 CORS
      // 改为调用 sync-master 无关端口。这里用 syncBills 前的说明即可。
      const res = await request.get('/integration/cmcloud/api-status').catch(() => null)
      if (res && typeof res.reachable === 'boolean') {
        setApiOk(res.reachable)
        return
      }
      setApiOk(null)
    } finally {
      setChecking(false)
    }
  }

  useEffect(() => {
    checkApi()
  }, [])

  const syncMaster = async () => {
    setSyncingMaster(true)
    try {
      const res = await cmcloudSyncApi.syncMaster()
      const tip = `客户 ${res?.customers ?? 0} / 比价 ${res?.priceCompare ?? 0} / 新开客户 ${res?.customerDev ?? 0}`
      setLastMasterTip(tip)
      message.success(`已写入本地库：${tip}`)
    } catch {
      // handled
    } finally {
      setSyncingMaster(false)
    }
  }

  const syncBills = async () => {
    setSyncingBills(true)
    try {
      const res = await cmcloudSyncApi.syncBills()
      const tip = `销售单 ${res?.saleBills ?? 0} / 库存 ${res?.inventoryUpdated ?? 0} / 排名 ${res?.salesRank ?? 0} / 在线 ${res?.onlineSale ?? 0} / 结构 ${res?.productStructure ?? 0} / 异地 ${res?.offsiteSale ?? 0} / 复购 ${res?.customerMaintainUpdated ?? 0}`
      setLastBillTip(`${res?.beginDate || ''} ~ ${res?.endDate || ''}｜${tip}`)
      setApiOk(true)
      message.success(`销售/库存已从管家婆写入本地库：${tip}`)
    } catch {
      setApiOk(false)
    } finally {
      setSyncingBills(false)
    }
  }

  const uploadSales = async (file) => {
    setImportingSales(true)
    try {
      const res = await cmcloudSyncApi.importSalesExcel(file)
      const tip = `单据 ${res?.saleBills ?? 0} / 排名 ${res?.salesRank ?? 0} / 在线 ${res?.onlineSale ?? 0} / 结构 ${res?.productStructure ?? 0} / 异地 ${res?.offsiteSale ?? 0}`
      setLastImportTip(`销售：${tip}`)
      message.success(`销售已写入本地库：${tip}`)
    } catch {
      // handled
    } finally {
      setImportingSales(false)
    }
    return false
  }

  const uploadInventory = async (file) => {
    setImportingInv(true)
    try {
      const res = await cmcloudSyncApi.importInventoryExcel(file)
      const tip = `库存 SKU ${res?.inventoryUpdated ?? 0}`
      setLastImportTip((prev) => (prev ? `${prev}；${tip}` : tip))
      message.success(`库存已写入本地库：${tip}`)
    } catch {
      // handled
    } finally {
      setImportingInv(false)
    }
    return false
  }

  return (
    <div>
      <Alert
        type={apiOk === false ? 'warning' : 'info'}
        showIcon
        style={{ marginBottom: 16 }}
        message="目标：从管家婆云财贸直接写入本平台数据库（不是每次手工上传）"
        description={
          <div>
            <Paragraph style={{ marginBottom: 8 }}>
              仓库字典（本机已能读到）：01番禺库、02后库、03花都库、04临汾库、05门店前厅、06活动用酒、07顺德库、08季度品鉴、09北京库、10赠饮活动、11二楼品鉴库。
              看板库存按确认口径汇总 <Text strong>02后库 + 05门店前厅</Text>；销售为全库房明细汇总。
            </Paragraph>
            <Paragraph style={{ marginBottom: 8 }}>
              <Text strong>为何现在不能自动拉销售/库存数量：</Text>
              这些数量只存在于管家婆云端账套。本机 BaseInfo 只有客户/商品/仓库主档，没有结存与销售流水。
              官方开放读取方式是「财贸 API」（本机 :45100 或云网关密钥）。未开通时，第三方程序无法像你在软件里点报表那样取数。
            </Paragraph>
            <Paragraph style={{ marginBottom: 0 }}>
              <Text strong>请你只做一次：</Text>
              在已登录的管家婆里打开 <Text code>应用中心</Text> → 安装/启用 <Text code>财贸API</Text>（在软件内开通，不是让你从外网下安装包给我）。
              开通后点下面「从管家婆拉取销售/库存」即可反复自动写入本地库。
              {apiOk === true ? <Tag color="success" style={{ marginLeft: 8 }}>API 可用</Tag> : null}
              {apiOk === false ? <Tag color="error" style={{ marginLeft: 8 }}>API 未连通</Tag> : null}
              <Button type="link" size="small" loading={checking} onClick={checkApi}>
                重新检测
              </Button>
            </Paragraph>
          </div>
        }
      />

      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Card
          title={
            <span>
              <ShoppingOutlined style={{ marginRight: 8 }} />
              一键从管家婆写入本地库（正式路径）
            </span>
          }
        >
          <Paragraph type="secondary">
            读取管家婆销售单（各库房）+ 存货结存（后库/前厅汇总），写入销售排名、在线销售、异地销售、库存、未复购等表。
          </Paragraph>
          <Space wrap>
            <Button type="primary" icon={<CloudSyncOutlined />} loading={syncingBills} onClick={syncBills}>
              从管家婆拉取销售/库存
            </Button>
            <Button icon={<DatabaseOutlined />} loading={syncingMaster} onClick={syncMaster}>
              同步基础资料（客户/比价）
            </Button>
          </Space>
          {lastBillTip ? (
            <Paragraph style={{ marginTop: 12, marginBottom: 0 }}>销售/库存：{lastBillTip}</Paragraph>
          ) : null}
          {lastMasterTip ? (
            <Paragraph style={{ marginTop: 8, marginBottom: 0 }}>基础资料：{lastMasterTip}</Paragraph>
          ) : null}
        </Card>

        <Card
          title={
            <span>
              <FileExcelOutlined style={{ marginRight: 8 }} />
              临时兜底：报表 Excel 导入（API 未开通时）
            </span>
          }
        >
          <Paragraph type="secondary">
            仅当财贸 API 暂未开通时使用。正式开通 API 后不必再上传。
            销售明细表 = 全库房销售；存货库存详情 = 选后库+门店前厅后导出。
          </Paragraph>
          <Space wrap>
            <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={uploadSales}>
              <Button icon={<FileExcelOutlined />} loading={importingSales}>
                上传销售明细表
              </Button>
            </Upload>
            <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={uploadInventory}>
              <Button icon={<FileExcelOutlined />} loading={importingInv}>
                上传存货库存详情
              </Button>
            </Upload>
          </Space>
          {lastImportTip ? (
            <Paragraph style={{ marginTop: 12, marginBottom: 0 }}>最近一次：{lastImportTip}</Paragraph>
          ) : null}
        </Card>
      </Space>
    </div>
  )
}
