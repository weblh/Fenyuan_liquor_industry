import { useCallback, useEffect, useState } from 'react'
import { Card, Form, Input, Button, Tag, Space, Modal, message, Popconfirm, Select, Table } from 'antd'
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
import { kingdeeCredentialApi } from '@/api/modules/kingdee'

const DEFAULT_URL = 'https://dichanerp.huaxianggroup.cn/k3cloud/'

export default function KingdeeCredentialPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [testingId, setTestingId] = useState(null)
  const [form] = Form.useForm()

  const fetchList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await kingdeeCredentialApi.list({ current: 1, size: 100 })
      setData(res?.records || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchList()
  }, [fetchList])

  const openModal = (record) => {
    setEditing(record || null)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record, password: '' })
    } else {
      form.setFieldsValue({ kingdeeUrl: DEFAULT_URL, status: 1 })
    }
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await form.validateFields()
    if (!editing && !values.password) {
      message.error('新建账号时密码不能为空')
      return
    }
    try {
      if (editing) {
        await kingdeeCredentialApi.update(editing.id, values)
        message.success('账号已更新')
      } else {
        await kingdeeCredentialApi.create(values)
        message.success('账号已添加')
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
      const res = await kingdeeCredentialApi.testLogin(id)
      message.success(res?.message || '登录成功')
    } catch {
      // handled by request
    } finally {
      setTestingId(null)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '账号名称', dataIndex: 'name', width: 140 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => <Tag color={Number(v) === 1 ? 'green' : 'red'}>{Number(v) === 1 ? '启用' : '禁用'}</Tag>,
    },
    { title: '金蝶地址', dataIndex: 'kingdeeUrl', ellipsis: true },
    { title: '用户名', dataIndex: 'username', width: 120 },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    { title: '更新时间', dataIndex: 'updateTime', width: 170 },
    {
      title: '操作',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space wrap>
          <Permission permission="kingdee:credential:edit">
            <Button type="link" icon={<EditOutlined />} onClick={() => openModal(record)}>编辑</Button>
          </Permission>
          <Permission permission="kingdee:credential:edit">
            <Button
              type="link"
              icon={<ApiOutlined />}
              loading={testingId === record.id}
              onClick={() => handleTestLogin(record.id)}
            >
              测试登录
            </Button>
          </Permission>
          <Permission permission="kingdee:credential:edit">
            <Button
              type="link"
              icon={Number(record.status) === 1 ? <StopOutlined /> : <CheckCircleOutlined />}
              onClick={async () => {
                await kingdeeCredentialApi.toggle(record.id)
                message.success('状态已切换')
                fetchList()
              }}
            >
              {Number(record.status) === 1 ? '禁用' : '启用'}
            </Button>
          </Permission>
          <Permission permission="kingdee:credential:delete">
            <Popconfirm
              title="确定删除此账号？"
              onConfirm={async () => {
                await kingdeeCredentialApi.remove(record.id)
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
        title="金蝶账号密码"
        extra={
          <Permission permission="kingdee:credential:add">
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>添加账号</Button>
          </Permission>
        }
      >
        <Table dataSource={data} columns={columns} loading={loading} rowKey="id" scroll={{ x: 1200 }} pagination={false} />
      </Card>

      <Modal
        title={editing ? '编辑金蝶账号' : '添加金蝶账号'}
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onSubmit}>
          <Form.Item label="账号名称" name="name" rules={[{ required: true, message: '请输入账号名称' }]}>
            <Input placeholder="如：黄增峰" />
          </Form.Item>
          <Form.Item label="金蝶地址" name="kingdeeUrl" rules={[{ required: true, message: '请输入金蝶地址' }]}>
            <Input placeholder={DEFAULT_URL} />
          </Form.Item>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="金蝶登录用户名" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={editing ? [] : [{ required: true, message: '请输入密码' }]}
            extra={editing ? '留空则保持原密码不变' : undefined}
          >
            <Input.Password placeholder={editing ? '留空则不修改' : '金蝶登录密码'} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={2} placeholder="备注说明（可选）" />
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
