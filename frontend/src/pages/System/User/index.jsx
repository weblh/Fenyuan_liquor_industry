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
  Tag,
} from 'antd'
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import {
  getUserList,
  addUser,
  updateUser,
  deleteUsers,
  updateUserStatus,
  resetUserPassword,
} from '@/api/modules/user'
import { getAllRoles } from '@/api/modules/role'
import { getDeptTree } from '@/api/modules/dept'
import Permission from '@/components/Permission'

/** 后端分页参数：current / size；状态：1 正常 0 停用 */
export default function UserPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ current: 1, size: 10 })
  const [searchForm] = Form.useForm()
  const [editForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [roles, setRoles] = useState([])
  const [depts, setDepts] = useState([])

  const fetchList = async (params = query) => {
    setLoading(true)
    try {
      const res = await getUserList(params)
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
    getAllRoles()
      .then((res) => setRoles(Array.isArray(res) ? res : []))
      .catch(() => {})
    getDeptTree()
      .then((res) => setDepts(Array.isArray(res) ? res : []))
      .catch(() => {})
  }, [])

  const onSearch = () => {
    const values = searchForm.getFieldsValue()
    fetchList({ ...query, current: 1, ...values })
  }

  const openModal = (record) => {
    setEditing(record || null)
    editForm.resetFields()
    if (record) {
      editForm.setFieldsValue({
        ...record,
        roleIds: record.roleIds || record.roles?.map((r) => r.id || r.roleId),
      })
    } else {
      editForm.setFieldsValue({ status: 1 })
    }
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await editForm.validateFields()
    try {
      if (editing) {
        await updateUser(editing.id, values)
        message.success('更新成功')
      } else {
        await addUser(values)
        message.success('新增成功')
      }
      setOpen(false)
      fetchList()
    } catch {
      // handled by request
    }
  }

  const onDelete = async (ids) => {
    await deleteUsers(ids)
    message.success('删除成功')
    fetchList()
  }

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
    { title: '手机号', dataIndex: 'phone', key: 'phone' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (v, record) => (
        <Tag
          color={v === 1 || v === '1' ? 'success' : 'error'}
          style={{ cursor: 'pointer' }}
          onClick={async () => {
            const next = v === 1 || v === '1' ? 0 : 1
            await updateUserStatus(record.id, next)
            message.success('状态已更新')
            fetchList()
          }}
        >
          {v === 1 || v === '1' ? '正常' : '停用'}
        </Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: '操作',
      key: 'action',
      width: 240,
      render: (_, record) => (
        <Space>
          <Permission permission="system:user:edit">
            <Button type="link" size="small" onClick={() => openModal(record)}>
              编辑
            </Button>
          </Permission>
          <Permission permission="system:user:resetPwd">
            <Button
              type="link"
              size="small"
              onClick={async () => {
                await resetUserPassword(record.id, { password: '123456' })
                message.success('密码已重置为 123456')
              }}
            >
              重置密码
            </Button>
          </Permission>
          <Permission permission="system:user:delete">
            <Popconfirm title="确认删除？" onConfirm={() => onDelete(record.id)}>
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
      <Form form={searchForm} layout="inline" style={{ marginBottom: 16 }} onFinish={onSearch}>
        <Form.Item name="username">
          <Input placeholder="用户名" allowClear />
        </Form.Item>
        <Form.Item name="status">
          <Select
            placeholder="状态"
            allowClear
            style={{ width: 120 }}
            options={[
              { value: 1, label: '正常' },
              { value: 0, label: '停用' },
            ]}
          />
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

      <div style={{ marginBottom: 12 }}>
        <Permission permission="system:user:add">
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal(null)}>
            新增
          </Button>
        </Permission>
      </div>

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
          showTotal: (t) => `共 ${t} 条`,
          onChange: (current, size) => fetchList({ ...query, current, size }),
        }}
      />

      <Modal
        title={editing ? '编辑用户' : '新增用户'}
        open={open}
        onOk={onSubmit}
        onCancel={() => setOpen(false)}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" initialValues={{ status: 1 }}>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input disabled={!!editing} />
          </Form.Item>
          {!editing && (
            <Form.Item name="password" label="密码" rules={[{ required: true }]}>
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item name="nickname" label="昵称">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item name="deptId" label="部门">
            <Select allowClear options={flattenDept(depts)} />
          </Form.Item>
          <Form.Item name="roleIds" label="角色">
            <Select
              mode="multiple"
              options={roles.map((r) => ({
                value: r.id,
                label: r.name,
              }))}
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

function flattenDept(nodes = [], prefix = '') {
  const result = []
  nodes.forEach((n) => {
    const label = `${prefix}${n.name}`
    result.push({ value: n.id, label })
    if (n.children?.length) {
      result.push(...flattenDept(n.children, `${label} / `))
    }
  })
  return result
}
