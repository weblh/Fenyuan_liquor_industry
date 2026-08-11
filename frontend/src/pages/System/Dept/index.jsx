import { useEffect, useState } from 'react'
import {
  Table,
  Form,
  Input,
  Button,
  Space,
  Modal,
  message,
  Select,
  Popconfirm,
  InputNumber,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { getDeptList, addDept, updateDept, deleteDepts } from '@/api/modules/dept'
import Permission from '@/components/Permission'

export default function DeptPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [editForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await getDeptList()
      setData(Array.isArray(res) ? res : [])
    } catch {
      setData([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
  }, [])

  const openModal = (record, parent) => {
    setEditing(record || null)
    editForm.resetFields()
    if (record) {
      editForm.setFieldsValue(record)
    } else {
      editForm.setFieldsValue({
        parentId: parent ? parent.id : 0,
        sort: 0,
        status: 1,
      })
    }
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await editForm.validateFields()
    if (editing) {
      await updateDept(editing.id, values)
      message.success('更新成功')
    } else {
      await addDept(values)
      message.success('新增成功')
    }
    setOpen(false)
    fetchList()
  }

  const columns = [
    { title: '部门名称', dataIndex: 'name', key: 'name' },
    { title: '编码', dataIndex: 'code', key: 'code', width: 100 },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
    { title: '负责人', dataIndex: 'leader', key: 'leader' },
    { title: '联系电话', dataIndex: 'phone', key: 'phone' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (v) => (v === 1 || v === '1' ? '正常' : '停用'),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_, record) => (
        <Space>
          <Permission permission="system:dept:add">
            <Button type="link" size="small" onClick={() => openModal(null, record)}>
              新增
            </Button>
          </Permission>
          <Permission permission="system:dept:edit">
            <Button type="link" size="small" onClick={() => openModal(record)}>
              编辑
            </Button>
          </Permission>
          <Permission permission="system:dept:delete">
            <Popconfirm
              title="确认删除？"
              onConfirm={async () => {
                await deleteDepts(record.id)
                message.success('删除成功')
                fetchList()
              }}
            >
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          </Permission>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <Permission permission="system:dept:add">
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal(null)}>
            新增
          </Button>
        </Permission>
        <Button icon={<ReloadOutlined />} onClick={fetchList}>
          刷新
        </Button>
      </Space>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data}
        pagination={false}
        defaultExpandAllRows
      />

      <Modal
        title={editing ? '编辑部门' : '新增部门'}
        open={open}
        onOk={onSubmit}
        onCancel={() => setOpen(false)}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="parentId" label="上级部门">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="name" label="部门名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="部门编码">
            <Input />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="leader" label="负责人">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="联系电话">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 1, label: '正常' },
                { value: 0, label: '停用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
