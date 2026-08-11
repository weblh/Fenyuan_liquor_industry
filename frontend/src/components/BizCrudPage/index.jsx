import { useEffect, useState } from 'react'
import { Table, Form, Input, Button, Space, Modal, message, Popconfirm, InputNumber, Select } from 'antd'
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import Permission from '@/components/Permission'

/**
 * 通用业务 CRUD 列表页
 * fields: [{ name, label, type: 'text'|'number'|'textarea'|'select', required, options, search, table, width, render }]
 */
export default function BizCrudPage({
  title,
  permissions,
  api,
  fields,
  rowKey = 'id',
}) {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ current: 1, size: 10 })
  const [searchForm] = Form.useForm()
  const [editForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const searchFields = fields.filter((f) => f.search)
  const formFields = fields.filter((f) => f.form !== false)
  const tableFields = fields.filter((f) => f.table !== false)

  const fetchList = async (params = query) => {
    setLoading(true)
    try {
      const res = await api.list(params)
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
    if (record) {
      editForm.setFieldsValue(record)
    }
    setOpen(true)
  }

  const onSubmit = async () => {
    const values = await editForm.validateFields()
    try {
      if (editing) {
        await api.update(editing.id, values)
        message.success('更新成功')
      } else {
        await api.create(values)
        message.success('新增成功')
      }
      setOpen(false)
      fetchList()
    } catch {
      // handled by request
    }
  }

  const onDelete = async (id) => {
    await api.remove(id)
    message.success('删除成功')
    fetchList()
  }

  const renderFormItem = (field) => {
    if (field.type === 'number') {
      return <InputNumber style={{ width: '100%' }} precision={field.precision ?? 2} min={field.min} />
    }
    if (field.type === 'textarea') {
      return <Input.TextArea rows={3} />
    }
    if (field.type === 'select') {
      return (
        <Select
          allowClear
          options={field.options || []}
          placeholder={`请选择${field.label}`}
        />
      )
    }
    return <Input allowClear placeholder={`请输入${field.label}`} />
  }

  const columns = [
    ...tableFields.map((f) => ({
      title: f.label,
      dataIndex: f.name,
      key: f.name,
      width: f.width,
      ellipsis: f.ellipsis,
      render: f.render,
    })),
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Permission permission={permissions.edit}>
            <Button type="link" size="small" onClick={() => openModal(record)}>
              编辑
            </Button>
          </Permission>
          <Permission permission={permissions.delete}>
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
      <Form
        form={searchForm}
        layout="inline"
        style={{ marginBottom: 16 }}
        onFinish={() => fetchList({ ...query, current: 1, ...searchForm.getFieldsValue() })}
      >
        {searchFields.map((f) => (
          <Form.Item key={f.name} name={f.name}>
            {f.type === 'select' ? (
              <Select
                allowClear
                style={{ width: 160 }}
                placeholder={f.label}
                options={f.options || []}
              />
            ) : (
              <Input placeholder={f.label} allowClear />
            )}
          </Form.Item>
        ))}
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
            <Permission permission={permissions.add}>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal(null)}>
                新增
              </Button>
            </Permission>
          </Space>
        </Form.Item>
      </Form>

      <Table
        rowKey={rowKey}
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 'max-content' }}
        pagination={{
          current: query.current,
          pageSize: query.size,
          total,
          showSizeChanger: true,
          onChange: (current, size) => fetchList({ ...query, current, size }),
        }}
      />

      <Modal
        title={editing ? `编辑${title}` : `新增${title}`}
        open={open}
        onOk={onSubmit}
        onCancel={() => setOpen(false)}
        destroyOnClose
        width={560}
      >
        <Form form={editForm} layout="vertical">
          {formFields.map((f) => (
            <Form.Item
              key={f.name}
              name={f.name}
              label={f.label}
              rules={f.required ? [{ required: true, message: `请输入${f.label}` }] : undefined}
            >
              {renderFormItem(f)}
            </Form.Item>
          ))}
        </Form>
      </Modal>
    </div>
  )
}
