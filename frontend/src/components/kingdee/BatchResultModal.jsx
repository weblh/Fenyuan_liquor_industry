import { Modal, Tag, Typography, Table } from 'antd'

export default function BatchResultModal({
  open,
  title,
  summary,
  items = [],
  serialLabel = '流水号',
  onClose,
}) {
  const columns = [
    {
      title: serialLabel,
      dataIndex: 'tradeSerialNo',
      width: 168,
      ellipsis: true,
      render: (v) => v || '-',
    },
    {
      title: '日期',
      dataIndex: 'bookkeepingDate',
      width: 100,
      render: (v) => v || '-',
    },
    {
      title: '金额',
      width: 110,
      render: (_, r) =>
        r.amount != null ? `${r.amountSide || ''}${r.amountSide ? ' ' : ''}${r.amount}` : '-',
    },
    {
      title: '结果',
      width: 110,
      render: (_, r) => {
        if (r.writable === true && (r.localOnly || r.message?.includes('预检通过'))) {
          return <Tag color="success">预检可写</Tag>
        }
        if (r.writable === false && r.message?.includes('档案缺失')) {
          return <Tag color="error">档案缺失</Tag>
        }
        if (r.matched === true) {
          return <Tag color="success">金蝶命中</Tag>
        }
        if (r.matched === false) {
          return <Tag color="error">未识别</Tag>
        }
        if (r.kingdeeExistStatus === 1) {
          return (
            <Tag color="error">
              重复
              {r.kingdeeExistVoucherNo
                ? ` ${r.kingdeeExistVoucherWord || ''}-${r.kingdeeExistVoucherNo}`
                : ''}
            </Tag>
          )
        }
        if (r.kingdeeExistStatus === 2) {
          return <Tag color="success">不重复</Tag>
        }
        return <Tag>详情</Tag>
      },
    },
    {
      title: '原因/说明',
      dataIndex: 'message',
      render: (v, r) => {
        const text = (v || r.voucherTypeName || '-').toString()
        return (
          <Typography.Paragraph copyable={text !== '-'} style={{ marginBottom: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
            {text}
          </Typography.Paragraph>
        )
      },
    },
  ]

  return (
    <Modal
      open={open}
      title={title}
      width={1100}
      onCancel={onClose}
      onOk={onClose}
      okText="关闭"
      cancelButtonProps={{ style: { display: 'none' } }}
      destroyOnClose
      styles={{ body: { maxHeight: '75vh', overflowY: 'auto' } }}
    >
      {summary?.lines?.length ? (
        <div style={{ marginBottom: 12 }}>
          {summary.lines.map((line) => (
            <Typography.Paragraph key={line} style={{ marginBottom: 4 }}>
              {line}
            </Typography.Paragraph>
          ))}
        </div>
      ) : null}
      {items.length > 0 ? (
        <Table
          rowKey={(r) => String(r.id ?? `${r.tradeSerialNo}-${r.message}`)}
          size="small"
          columns={columns}
          dataSource={items}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          scroll={{ y: 480, x: 1000 }}
        />
      ) : (
        <Typography.Text type="secondary">无明细</Typography.Text>
      )}
    </Modal>
  )
}
