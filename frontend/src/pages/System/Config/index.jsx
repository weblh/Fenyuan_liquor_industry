import { useEffect, useState } from 'react'
import { Table, Form, Input, Button, Space, Modal, message } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { getConfigList, updateConfig } from '@/api/modules/config'
import Permission from '@/components/Permission'

export default function ConfigPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ current: 1, size: 10 })
  const [searchForm] = Form.useForm()
  const [editForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const fetchList = async (params = query) => {
    setLoading(true)
    try {
      const res = await getConfigList(params)
      const list = res?.records || res?.list || (Array.isArray(res) ? res : [])
      setData(list)
      setTotal(res?.total ?? list.length)
      setQuery(params)
    } catch {
      setData([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
  }, [])

  const openModal = (record) => {
    setEditing(record)
    editForm.resetFields()
    editForm.setFieldsValue(record)
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await editForm.validateFields()
    await updateConfig(editing.id, values)
    message.success('更新成功')
    setOpen(false)
    fetchList()
  }

  const columns = [
    { title: '参数名称', dataIndex: 'name', key: 'name' },
    { title: '参数键名', dataIndex: 'configKey', key: 'configKey' },
    { title: '参数键值', dataIndex: 'configValue', key: 'configValue', ellipsis: true },
    { title: '分组', dataIndex: 'groupName', key: 'groupName', width: 120 },
    { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Permission permission="system:config:edit">
          <Button type="link" size="small" onClick={() => openModal(record)}>
            编辑
          </Button>
        </Permission>
      ),
    },
  ]

  return (
    <div>
      <Form
        form={searchForm}
        layout="inline"
        style={{ marginBottom: 16 }}
        onFinish={() => fetchList({ ...query, current: 1, ...searchForm.getFieldsValue() })}
      >
        <Form.Item name="name">
          <Input placeholder="参数名称" allowClear />
        </Form.Item>
        <Form.Item name="groupName">
          <Input placeholder="分组" allowClear />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              搜索
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                searchForm.resetFields()
                fetchList({ current: 1, size: 10 })
              }}
            >
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data}
        pagination={{
          current: query.current,
          pageSize: query.size,
          total,
          showSizeChanger: true,
          onChange: (current, size) => fetchList({ ...query, current, size }),
        }}
      />

      <Modal title="编辑参数" open={open} onOk={onSubmit} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label="参数名称">
            <Input disabled />
          </Form.Item>
          <Form.Item name="configKey" label="参数键名">
            <Input disabled />
          </Form.Item>
          <Form.Item name="configValue" label="参数键值" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
