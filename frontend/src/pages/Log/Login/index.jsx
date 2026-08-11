import { useEffect, useState } from 'react'
import { Table, Form, Input, Button, Space, Select } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { getLoginLogList } from '@/api/modules/log'

export default function LoginLogPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ current: 1, size: 10 })
  const [searchForm] = Form.useForm()

  const fetchList = async (params = query) => {
    setLoading(true)
    try {
      const res = await getLoginLogList(params)
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

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '登录地址', dataIndex: 'ip', key: 'ip' },
    { title: '登录地点', dataIndex: 'location', key: 'location' },
    { title: '浏览器', dataIndex: 'browser', key: 'browser' },
    { title: '操作系统', dataIndex: 'os', key: 'os' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (v) => (v === 1 || v === '1' ? '成功' : '失败'),
    },
    { title: '提示消息', dataIndex: 'msg', key: 'msg', ellipsis: true },
    { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 180 },
  ]

  return (
    <div>
      <Form
        form={searchForm}
        layout="inline"
        style={{ marginBottom: 16 }}
        onFinish={() => fetchList({ ...query, current: 1, ...searchForm.getFieldsValue() })}
      >
        <Form.Item name="username">
          <Input placeholder="用户名" allowClear />
        </Form.Item>
        <Form.Item name="status">
          <Select
            placeholder="状态"
            allowClear
            style={{ width: 120 }}
            options={[
              { value: 1, label: '成功' },
              { value: 0, label: '失败' },
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
    </div>
  )
}
