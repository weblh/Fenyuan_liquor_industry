import { useEffect, useState } from 'react'
import { Table, Form, Input, Button, Space, Modal } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { getOperLogList } from '@/api/modules/log'

export default function OperLogPage() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ current: 1, size: 10 })
  const [searchForm] = Form.useForm()
  const [detail, setDetail] = useState(null)

  const fetchList = async (params = query) => {
    setLoading(true)
    try {
      const res = await getOperLogList(params)
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
    { title: '模块', dataIndex: 'module', key: 'module' },
    { title: '操作', dataIndex: 'operation', key: 'operation' },
    { title: '操作人员', dataIndex: 'username', key: 'username' },
    { title: '请求方法', dataIndex: 'method', key: 'method', ellipsis: true },
    { title: 'IP', dataIndex: 'ip', key: 'ip', width: 130 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (v) => (v === 1 || v === '1' ? '成功' : '失败'),
    },
    { title: '耗时(ms)', dataIndex: 'time', key: 'time', width: 90 },
    { title: '操作时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => setDetail(record)}>
          详情
        </Button>
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
        <Form.Item name="module">
          <Input placeholder="系统模块" allowClear />
        </Form.Item>
        <Form.Item name="username">
          <Input placeholder="操作人员" allowClear />
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

      <Modal
        title="操作日志详情"
        open={!!detail}
        onCancel={() => setDetail(null)}
        footer={null}
        width={720}
      >
        {detail && (
          <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: 480, overflow: 'auto' }}>
            {JSON.stringify(detail, null, 2)}
          </pre>
        )}
      </Modal>
    </div>
  )
}
