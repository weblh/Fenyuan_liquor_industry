import { useCallback, useEffect, useState } from 'react'
import { Card, Form, Input, Button, Tag, Space, Modal, message, Popconfirm, Select, Table, Switch } from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ApiOutlined,
  CheckCircleOutlined,
  StopOutlined,
  StarOutlined,
} from '@ant-design/icons'
import Permission from '@/components/Permission'
import { kingdeeAccountSetApi, kingdeeCredentialApi } from '@/api/modules/kingdee'

export default function KingdeeAccountSetPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [credentials, setCredentials] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [testingId, setTestingId] = useState(null)
  const [form] = Form.useForm()

  const fetchList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await kingdeeAccountSetApi.list({ current: 1, size: 100 })
      setData(res?.records || [])
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchCredentials = useCallback(async () => {
    try {
      const list = await kingdeeCredentialApi.listEnabled()
      setCredentials(Array.isArray(list) ? list : [])
    } catch {
      setCredentials([])
    }
  }, [])

  useEffect(() => {
    fetchList()
    fetchCredentials()
  }, [fetchList, fetchCredentials])

  const openModal = (record) => {
    setEditing(record || null)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record, isDefault: Number(record.isDefault) === 1 })
    } else {
      form.setFieldsValue({
        defaultFormId: 'GL_VOUCHER',
        status: 1,
        isDefault: false,
        credentialId: credentials[0]?.id,
      })
    }
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await form.validateFields()
    const payload = { ...values, isDefault: values.isDefault ? 1 : 0 }
    try {
      if (editing) {
        await kingdeeAccountSetApi.update(editing.id, payload)
        message.success('账套已更新')
      } else {
        await kingdeeAccountSetApi.create(payload)
        message.success('账套已添加')
      }
      setOpen(false)
      fetchList()
    } catch {
      // handled by request
    }
  }

  const handleTestLogin = async (id) => {
    setTestingId(id)
    try {
      const res = await kingdeeAccountSetApi.testLogin(id)
      message.success(res?.message || '登录成功')
    } catch {
      // handled by request
    } finally {
      setTestingId(null)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '账套名称', dataIndex: 'accountName', width: 180 },
    { title: '关联账号', dataIndex: 'credentialName', width: 120 },
    {
      title: '默认',
      dataIndex: 'isDefault',
      width: 80,
      render: (v) => (Number(v) === 1 ? <Tag color="gold">默认</Tag> : null),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => <Tag color={Number(v) === 1 ? 'green' : 'red'}>{Number(v) === 1 ? '启用' : '禁用'}</Tag>,
    },
    { title: '账套ID', dataIndex: 'dbId', width: 140, ellipsis: true },
    { title: '公司编码', dataIndex: 'orgCompanyCode', width: 100 },
    { title: '使用组织', dataIndex: 'useOrgCode', width: 100 },
    { title: '银行账号', dataIndex: 'bankAccountNo', width: 160 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    {
      title: '操作',
      width: 340,
      fixed: 'right',
      render: (_, record) => (
        <Space wrap>
          <Permission permission="kingdee:accountSet:edit">
            <Button type="link" icon={<EditOutlined />} onClick={() => openModal(record)}>编辑</Button>
          </Permission>
          <Permission permission="kingdee:accountSet:edit">
            <Button
              type="link"
              icon={<ApiOutlined />}
              loading={testingId === record.id}
              onClick={() => handleTestLogin(record.id)}
            >
              测试登录
            </Button>
          </Permission>
          {Number(record.isDefault) !== 1 && (
            <Permission permission="kingdee:accountSet:edit">
              <Button
                type="link"
                icon={<StarOutlined />}
                onClick={async () => {
                  await kingdeeAccountSetApi.setDefault(record.id)
                  message.success('已设为默认账套')
                  fetchList()
                }}
              >
                设为默认
              </Button>
            </Permission>
          )}
          <Permission permission="kingdee:accountSet:edit">
            <Button
              type="link"
              icon={Number(record.status) === 1 ? <StopOutlined /> : <CheckCircleOutlined />}
              onClick={async () => {
                await kingdeeAccountSetApi.toggle(record.id)
                message.success('状态已切换')
                fetchList()
              }}
            >
              {Number(record.status) === 1 ? '禁用' : '启用'}
            </Button>
          </Permission>
          <Permission permission="kingdee:accountSet:delete">
            <Popconfirm
              title="确定删除此账套？"
              onConfirm={async () => {
                await kingdeeAccountSetApi.remove(record.id)
                message.success('已删除')
                fetchList()
              }}
            >
              <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          </Permission>
        </Space>
      ),
    },
  ]

  return (
    <div className="page-container">
      <Card
        title="金蝶账套"
        extra={
          <Permission permission="kingdee:accountSet:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>添加账套</Button>
          </Permission>
        }
      >
        <Table dataSource={data} columns={columns} loading={loading} rowKey="id" scroll={{ x: 1600 }} pagination={false} />
      </Card>

      <Modal
        title={editing ? '编辑金蝶账套' : '添加金蝶账套'}
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onSubmit}>
          <Form.Item label="关联账号" name="credentialId" rules={[{ required: true, message: '请选择关联账号' }]}>
            <Select
              placeholder="选择金蝶登录账号"
              options={credentials.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          <Form.Item label="账套名称" name="accountName" rules={[{ required: true, message: '请输入账套名称' }]}>
            <Input placeholder="如：广东汾源酒业有限公司" />
          </Form.Item>
          <Form.Item label="账套ID (dbId)" name="dbId" rules={[{ required: true, message: '请输入账套ID' }]}>
            <Input placeholder="金蝶数据中心ID" />
          </Form.Item>
          <Form.Item label="公司/账簿编码" name="orgCompanyCode">
            <Input placeholder="如 11（对应组织111）" />
          </Form.Item>
          <Form.Item label="使用组织编码" name="useOrgCode">
            <Input placeholder="用于供应商查询等，可选" />
          </Form.Item>
          <Form.Item label="银行账号" name="bankAccountNo">
            <Input placeholder="建行账号，如 719878715101" />
          </Form.Item>
          <Form.Item label="默认表单ID" name="defaultFormId">
            <Input placeholder="GL_VOUCHER" />
          </Form.Item>
          <Form.Item label="默认会计期间" name="defaultAccountingPeriod">
            <Input placeholder="如 202607" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={2} placeholder="备注说明（可选）" />
          </Form.Item>
          <Form.Item label="设为默认账套" name="isDefault" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select options={[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">{editing ? '保存修改' : '添加'}</Button>
              <Button onClick={() => setOpen(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
