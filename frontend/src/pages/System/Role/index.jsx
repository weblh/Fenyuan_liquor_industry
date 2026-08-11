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
  Tree,
  InputNumber,
} from 'antd'
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import {
  getRoleList,
  addRole,
  updateRole,
  deleteRoles,
  getRolePermissions,
  assignRolePermissions,
} from '@/api/modules/role'
import { getMenuList } from '@/api/modules/menu'
import Permission from '@/components/Permission'

export default function RolePage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ current: 1, size: 10 })
  const [searchForm] = Form.useForm()
  const [editForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [permOpen, setPermOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [menuTree, setMenuTree] = useState([])
  const [checkedKeys, setCheckedKeys] = useState([])
  const [halfCheckedKeys, setHalfCheckedKeys] = useState([])
  const [permRole, setPermRole] = useState(null)

  const fetchList = async (params = query) => {
    setLoading(true)
    try {
      const res = await getRoleList(params)
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
    setEditing(record || null)
    editForm.resetFields()
    if (record) editForm.setFieldsValue(record)
    else editForm.setFieldsValue({ status: 1, sort: 0 })
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await editForm.validateFields()
    if (editing) {
      await updateRole(editing.id, values)
      message.success('更新成功')
    } else {
      await addRole(values)
      message.success('新增成功')
    }
    setOpen(false)
    fetchList()
  }

  const openPerm = async (record) => {
    setPermRole(record)
    const menus = await getMenuList()
    setMenuTree(toTreeData(Array.isArray(menus) ? menus : []))
    const perms = await getRolePermissions(record.id)
    const keys = Array.isArray(perms) ? perms : perms?.menuIds || perms?.checkedKeys || []
    setCheckedKeys(keys.map(String))
    setHalfCheckedKeys([])
    setPermOpen(true)
  }

  const savePerm = async () => {
    const all = [...new Set([...checkedKeys, ...halfCheckedKeys])].map(Number)
    await assignRolePermissions(permRole.id, all)
    message.success('权限已保存')
    setPermOpen(false)
  }

  const columns = [
    { title: '角色名称', dataIndex: 'name', key: 'name' },
    { title: '角色编码', dataIndex: 'code', key: 'code' },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (v) => (
        <Tag color={v === 1 || v === '1' ? 'success' : 'error'}>
          {v === 1 || v === '1' ? '正常' : '停用'}
        </Tag>
      ),
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true, render: (v, r) => v || r.description },
    {
      title: '操作',
      key: 'action',
      width: 240,
      render: (_, record) => (
        <Space>
          <Permission permission="system:role:edit">
            <Button type="link" size="small" onClick={() => openModal(record)}>
              编辑
            </Button>
          </Permission>
          <Permission permission="system:role:permission">
            <Button type="link" size="small" onClick={() => openPerm(record)}>
              分配权限
            </Button>
          </Permission>
          <Permission permission="system:role:delete">
            <Popconfirm
              title="确认删除？"
              onConfirm={async () => {
                await deleteRoles(record.id)
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
      <Form
        form={searchForm}
        layout="inline"
        style={{ marginBottom: 16 }}
        onFinish={() => fetchList({ ...query, current: 1, ...searchForm.getFieldsValue() })}
      >
        <Form.Item name="name">
          <Input placeholder="角色名称" allowClear />
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
        <Permission permission="system:role:add">
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
          onChange: (current, size) => fetchList({ ...query, current, size }),
        }}
      />

      <Modal
        title={editing ? '编辑角色' : '新增角色'}
        open={open}
        onOk={onSubmit}
        onCancel={() => setOpen(false)}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" initialValues={{ status: 1, sort: 0 }}>
          <Form.Item name="name" label="角色名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="角色编码" rules={[{ required: true }]}>
            <Input placeholder="如 ROLE_USER" />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 1, label: '正常' },
                { value: 0, label: '停用' },
              ]}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`分配权限 - ${permRole?.name || ''}`}
        open={permOpen}
        onOk={savePerm}
        onCancel={() => setPermOpen(false)}
        width={520}
        destroyOnClose
      >
        <Tree
          checkable
          checkStrictly={false}
          defaultExpandAll
          checkedKeys={checkedKeys}
          onCheck={(keys, info) => {
            setCheckedKeys(keys.checked || keys)
            setHalfCheckedKeys(info.halfCheckedKeys || [])
          }}
          treeData={menuTree}
          fieldNames={{ title: 'title', key: 'key', children: 'children' }}
        />
      </Modal>
    </div>
  )
}

function toTreeData(nodes = []) {
  return nodes.map((n) => ({
    key: String(n.id),
    title: n.name,
    children: n.children?.length ? toTreeData(n.children) : undefined,
  }))
}
