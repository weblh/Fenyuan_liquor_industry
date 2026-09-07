import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, Select, Space, Upload, message, Tag, Table, Modal } from 'antd'
import {
  BankOutlined,
  CloudSyncOutlined,
  SearchOutlined,
  UploadOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
  DeleteOutlined,
} from '@ant-design/icons'
import Permission from '@/components/Permission'
import { useKingdeeAccountSelect } from '@/hooks/useKingdeeAccountSelect'
import BatchResultModal from '@/components/kingdee/BatchResultModal'
import {
  checkBankKingdeeExists,
  clearAllBankVouchers,
  importBankVouchers,
  inferBankVoucherTypes,
  listBankChannels,
  searchBankVouchers,
  writeBankVouchers,
} from '@/api/modules/bankVoucher'

const existStatusMap = {
  0: { color: 'default', text: '未检查' },
  1: { color: 'error', text: '金蝶已有' },
  2: { color: 'success', text: '金蝶无' },
}

const writeStatusMap = {
  0: { color: 'default', text: '未写入' },
  1: { color: 'success', text: '已写入' },
  2: { color: 'error', text: '失败' },
}

export default function BankVoucherPage() {
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [data, setData] = useState([])
  const [channels, setChannels] = useState([])
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [resultOpen, setResultOpen] = useState(false)
  const [resultTitle, setResultTitle] = useState('')
  const [resultSummary, setResultSummary] = useState()
  const [resultItems, setResultItems] = useState([])
  const [form] = Form.useForm()
  const { accounts, loading: accountsLoading, setSelectedAccountId, selectedAccount } =
    useKingdeeAccountSelect()

  const loadChannels = useCallback(async () => {
    try {
      const list = await listBankChannels()
      setChannels(list || [])
      if (list?.length && !form.getFieldValue('channelKey')) {
        form.setFieldValue('channelKey', list[0].channelKey)
      }
    } catch {
      /* interceptor */
    }
  }, [form])

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const values = form.getFieldsValue()
      const rows = await searchBankVouchers({
        channelKey: values.channelKey,
        bookkeepingDateStart: values.bookkeepingDateStart,
        bookkeepingDateEnd: values.bookkeepingDateEnd,
        summary: values.summary,
        counterpartyName: values.counterpartyName,
      })
      setData(rows || [])
      setSelectedRowKeys([])
    } catch {
      /* interceptor */
    } finally {
      setLoading(false)
    }
  }, [form])

  useEffect(() => {
    loadChannels().then(loadData)
  }, [loadChannels, loadData])

  useEffect(() => {
    if (selectedAccount?.id) {
      form.setFieldValue('accountId', selectedAccount.id)
    }
  }, [selectedAccount, form])

  const selectedIds = selectedRowKeys.map((k) => Number(k))
  const scopeIds = data.map((d) => d.id).filter(Boolean)
  const accountId = Form.useWatch('accountId', form) || selectedAccount?.id
  const channelKey = Form.useWatch('channelKey', form)

  const isAlreadyInKingdee = (row) => row.kingdeeExistStatus === 1

  const writtenVoucherLabel = (row) => {
    if (row.writeStatus === 1 && row.kingdeeVoucherNo) {
      return `${row.kingdeeVoucherWord || '银'}-${row.kingdeeVoucherNo}`
    }
    if (row.kingdeeExistStatus === 1 && row.kingdeeExistVoucherNo) {
      return `${row.kingdeeExistVoucherWord || '银'}-${row.kingdeeExistVoucherNo}`
    }
    return ''
  }

  const writableSelectedIds = selectedIds.filter((id) => {
    const row = data.find((d) => d.id === id)
    return row && !isAlreadyInKingdee(row)
  })

  const showResult = (title, summary, items = []) => {
    setResultTitle(title)
    setResultSummary(summary)
    setResultItems(items)
    setResultOpen(true)
  }

  const runClearAll = () => {
    Modal.confirm({
      title: '一键清空确认',
      content: '将物理删除银行流水明细表中的全部数据，此操作不可恢复。是否继续？',
      okText: '确认清空',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        setActionLoading(true)
        try {
          const res = await clearAllBankVouchers()
          message.success(`已清空，物理删除 ${res?.count ?? 0} 条`)
          await loadData()
        } finally {
          setActionLoading(false)
        }
      },
    })
  }

  const runInfer = async () => {
    if (!scopeIds.length) {
      message.warning('请先导入或查询银行流水')
      return
    }
    setActionLoading(true)
    try {
      const result = await inferBankVoucherTypes({ ids: scopeIds, accountId, channelKey })
      showResult(
        '金蝶反推类型结果',
        {
          title: '反推完成',
          lines: [
            `合计 ${result.total} 条：匹配成功 ${result.matched}，匹配失败 ${result.unmatched}，已更新 ${result.updated}`,
            result.writableMatched != null
              ? `可写入 ${result.writableMatched}，不可写入 ${result.nonWritableMatched ?? 0}`
              : '',
            result.masterDataBlocked ? `档案缺失 ${result.masterDataBlocked} 条` : '',
          ].filter(Boolean),
        },
        result.items,
      )
      await loadData()
    } finally {
      setActionLoading(false)
    }
  }

  const runCheck = async () => {
    if (!scopeIds.length) {
      message.warning('请先导入或查询银行流水')
      return
    }
    setActionLoading(true)
    try {
      const result = await checkBankKingdeeExists({ ids: scopeIds, accountId, channelKey })
      showResult(
        '检查金蝶重复结果',
        {
          title: '检查完成',
          lines: [
            `合计 ${result.total} 条：金蝶已存在 ${result.existsCount} 条，不存在 ${result.notExistsCount} 条`,
          ],
        },
        result.items,
      )
      await loadData()
    } finally {
      setActionLoading(false)
    }
  }

  const runWrite = async () => {
    if (!writableSelectedIds.length) {
      message.warning(selectedIds.length ? '勾选的明细均为金蝶已有，无需重复写入' : '请先勾选要写入金蝶的明细')
      return
    }
    if (!accountId) {
      message.warning('请选择金蝶账套')
      return
    }
    setActionLoading(true)
    try {
      const result = await writeBankVouchers({ ids: writableSelectedIds, accountId, channelKey })
      showResult(
        '写入金蝶结果',
        {
          title: '写入完成',
          lines: [
            `成功 ${result.successCount ?? 0} 条，失败 ${result.failCount ?? 0} 条，跳过 ${result.skippedCount ?? 0} 条`,
            result.message || '',
          ].filter(Boolean),
        },
        [],
      )
      setSelectedRowKeys([])
      await loadData()
    } catch (e) {
      showResult(
        '写入金蝶结果',
        {
          title: '写入失败',
          lines: [e?.message || '写入失败，请查看后端日志'],
        },
        [],
      )
    } finally {
      setActionLoading(false)
    }
  }

  const columns = [
    { title: '记账日期', dataIndex: 'bookkeepingDate', width: 110 },
    { title: '交易时间', dataIndex: 'tradeTime', width: 150, ellipsis: true },
    { title: '流水号', dataIndex: 'tradeSerialNo', width: 140, ellipsis: true },
    {
      title: '借方',
      dataIndex: 'debitAmount',
      width: 100,
      align: 'right',
      render: (v) => (v != null ? Number(v).toFixed(2) : ''),
    },
    {
      title: '贷方',
      dataIndex: 'creditAmount',
      width: 100,
      align: 'right',
      render: (v) => (v != null ? Number(v).toFixed(2) : ''),
    },
    { title: '对方户名', dataIndex: 'counterpartyName', width: 160, ellipsis: true },
    { title: '摘要', dataIndex: 'summary', width: 140, ellipsis: true },
    {
      title: '凭证类型',
      dataIndex: 'voucherTypeName',
      width: 120,
      render: (v, r) => v || r.voucherType || '-',
    },
    {
      title: '金蝶重复',
      dataIndex: 'kingdeeExistStatus',
      width: 110,
      render: (v, r) => {
        const meta = existStatusMap[v ?? 0] || existStatusMap[0]
        const tip =
          v === 1 && r.kingdeeExistVoucherNo
            ? `${r.kingdeeExistVoucherWord || ''}-${r.kingdeeExistVoucherNo}`
            : meta.text
        return <Tag color={meta.color}>{tip}</Tag>
      },
    },
    {
      title: '写入状态',
      dataIndex: 'writeStatus',
      width: 120,
      render: (_, r) => {
        const voucher = writtenVoucherLabel(r)
        if (voucher) return <Tag color="success">{voucher}</Tag>
        if (r.writeStatus === 2) return <Tag color="error">失败</Tag>
        const meta = writeStatusMap[r.writeStatus ?? 0] || writeStatusMap[0]
        return <Tag color={meta.color}>{meta.text}</Tag>
      },
    },
  ]

  return (
    <Card
      title={
        <span>
          <BankOutlined style={{ marginRight: 8 }} />
          银行流水凭证
        </span>
      }
    >
      <Form form={form} layout="inline" style={{ marginBottom: 16, rowGap: 12 }}>
        <Form.Item name="channelKey" label="渠道">
          <Select
            style={{ width: 220 }}
            options={channels.map((c) => ({
              value: c.channelKey,
              label: `${c.companyName}-${c.bankName}`,
            }))}
          />
        </Form.Item>
        <Form.Item name="accountId" label="金蝶账套">
          <Select
            style={{ width: 220 }}
            loading={accountsLoading}
            placeholder="选择账套"
            options={accounts.map((item) => ({
              value: item.id,
              label: Number(item.isDefault) === 1 ? `${item.accountName}（默认）` : item.accountName,
            }))}
            onChange={(id) => setSelectedAccountId(id)}
          />
        </Form.Item>
        <Form.Item name="bookkeepingDateStart" label="记账起">
          <Input placeholder="yyyy-MM-dd" style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="bookkeepingDateEnd" label="记账止">
          <Input placeholder="yyyy-MM-dd" style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="summary" label="摘要">
          <Input allowClear style={{ width: 140 }} />
        </Form.Item>
        <Form.Item>
          <Space wrap>
            <Button type="primary" icon={<SearchOutlined />} onClick={loadData} loading={loading}>
              查询
            </Button>
            <Permission permission="kingdee:bankVoucher:import">
              <Upload
                accept=".xls,.xlsx"
                showUploadList={false}
                beforeUpload={async (file) => {
                  try {
                    const res = await importBankVouchers(file, form.getFieldValue('channelKey'))
                    message.success(res?.count != null ? `导入成功，共 ${res.count} 条` : '导入成功')
                    await loadData()
                  } catch {
                    /* interceptor */
                  }
                  return false
                }}
              >
                <Button icon={<UploadOutlined />}>导入流水</Button>
              </Upload>
            </Permission>
            <Button danger icon={<DeleteOutlined />} loading={actionLoading} onClick={runClearAll}>
              一键清空
            </Button>
            <Button icon={<CloudSyncOutlined />} loading={actionLoading} onClick={runInfer}>
              金蝶反推类型
            </Button>
            <Button icon={<CheckCircleOutlined />} loading={actionLoading} onClick={runCheck}>
              检查金蝶重复
            </Button>
            <Permission permission="kingdee:bankVoucher:write">
              <Button
                type="primary"
                icon={<CloudUploadOutlined />}
                loading={actionLoading}
                onClick={runWrite}
                disabled={!writableSelectedIds.length}
              >
                写入金蝶{writableSelectedIds.length ? ` (${writableSelectedIds.length})` : ''}
              </Button>
            </Permission>
          </Space>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        size="small"
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 1300 }}
        pagination={{ pageSize: 50, showSizeChanger: true }}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => {
            const allowed = keys.filter((k) => {
              const row = data.find((d) => d.id === k)
              return row && !isAlreadyInKingdee(row)
            })
            setSelectedRowKeys(allowed)
          },
          getCheckboxProps: (record) => ({
            disabled: isAlreadyInKingdee(record),
          }),
        }}
      />

      <BatchResultModal
        open={resultOpen}
        title={resultTitle}
        summary={resultSummary}
        items={resultItems}
        serialLabel="流水号"
        onClose={() => setResultOpen(false)}
      />
    </Card>
  )
}
