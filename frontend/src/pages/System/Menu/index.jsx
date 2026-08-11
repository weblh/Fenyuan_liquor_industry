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
import { getMenuList, addMenu, updateMenu, deleteMenus } from '@/api/modules/menu'
import Permission from '@/components/Permission'

const MENU_TYPES = [
  { value: 0, label: '目录' },
  { value: 1, label: '菜单' },
  { value: 2, label: '按钮' },
]

export default function MenuPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [editForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await getMenuList()
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
    } else if (parent) {
      editForm.setFieldsValue({
        parentId: parent.id,
        type: 1,
        sort: 0,
        visible: 1,
        status: 1,
      })
    } else {
      editForm.setFieldsValue({
        parentId: 0,
        type: 0,
        sort: 0,
        visible: 1,
        status: 1,
      })
    }
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await editForm.validateFields()
    if (editing) {
      await updateMenu(editing.id, values)
      message.success('更新成功')
    } else {
      await addMenu(values)
      message.success('新增成功')
    }
    setOpen(false)
    fetchList()
  }

  const columns = [
    { title: '菜单名称', dataIndex: 'name', key: 'name' },
    { title: '图标', dataIndex: 'icon', key: 'icon', width: 140 },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
    { title: '权限标识', dataIndex: 'permission', key: 'permission' },
    { title: '路由', dataIndex: 'path', key: 'path' },
    { title: '组件', dataIndex: 'component', key: 'component' },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 80,
      render: (v) => MENU_TYPES.find((t) => t.value === v)?.label || v,
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_, record) => (
        <Space>
          <Permission permission="system:menu:add">
            <Button type="link" size="small" onClick={() => openModal(null, record)}>
              新增
            </Button>
          </Permission>
          <Permission permission="system:menu:edit">
            <Button type="link" size="small" onClick={() => openModal(record)}>
              编辑
            </Button>
          </Permission>
          <Permission permission="system:menu:delete">
            <Popconfirm
              title="确认删除？"
              onConfirm={async () => {
                await deleteMenus(record.id)
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
        <Permission permission="system:menu:add">
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
        title={editing ? '编辑菜单' : '新增菜单'}
        open={open}
        onOk={onSubmit}
        onCancel={() => setOpen(false)}
        destroyOnClose
        width={640}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="parentId" label="上级菜单" initialValue={0}>
            <InputNumber style={{ width: '100%' }} placeholder="0 表示顶级" />
          </Form.Item>
          <Form.Item name="type" label="菜单类型" rules={[{ required: true }]}>
            <Select options={MENU_TYPES} />
          </Form.Item>
          <Form.Item name="name" label="菜单名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="path" label="路由地址">
            <Input />
          </Form.Item>
          <Form.Item name="component" label="组件路径">
            <Input />
          </Form.Item>
          <Form.Item name="permission" label="权限标识">
            <Input placeholder="如 system:user:list" />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input placeholder="Ant Design 图标名，如 UserOutlined" />
          </Form.Item>
          <Form.Item name="visible" label="显示">
            <Select
              options={[
                { value: 1, label: '显示' },
                { value: 0, label: '隐藏' },
              ]}
            />
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
