import { Alert } from 'antd'
import BizCrudPage from '@/components/BizCrudPage'
import { customerMaintainApi } from '@/api/modules/business'
import styles from './index.module.css'

const ALERT_DAYS = 60

/** 根据未复购天数返回徽章样式分级 */
function daysTier(days) {
  if (days == null || Number.isNaN(Number(days))) return 'badgeMild'
  const d = Number(days)
  if (d >= 200) return 'badgeHot'
  if (d >= 150) return 'badgeHigh'
  if (d >= 100) return 'badgeWarn'
  if (d >= 60) return 'badgeMild'
  return 'badgeSafe'
}

export default function CustomerMaintainPage() {
  return (
    <div className={styles.wrap}>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16, borderRadius: 10 }}
        message={<span style={{ fontWeight: 600 }}>客户维护监管</span>}
        description="客户数据来自管家婆 ERP 同步。未复购天数根据 ERP 最近购买日计算，无购买记录时显示为「-」而非 0 天。"
      />
      <BizCrudPage
        title="客户维护监管"
        permissions={{
          add: 'business:customerMaintain:add',
          edit: 'business:customerMaintain:edit',
          delete: 'business:customerMaintain:delete',
        }}
        api={customerMaintainApi}
        tableClassName={styles.table}
        fields={[
          { name: 'customerName', label: '客户名称', type: 'text', required: true, search: true, width: 200 },
          { name: 'contactPhone', label: '联系电话', type: 'text', width: 140 },
          { name: 'lastPurchaseDate', label: '最近购买日', type: 'text', width: 140 },
          {
            name: 'daysSincePurchase',
            label: '未复购天数',
            type: 'number',
            width: 160,
            render: (v) => {
              if (v == null || v === '') return <span className={`${styles.daysBadge} ${styles.badgeMild}`}>-</span>
              const days = Number(v)
              const tier = daysTier(days)
              const alert = days >= ALERT_DAYS
              return (
                <span className={`${styles.daysBadge} ${styles[tier]}`}>
                  <i className={styles.dot} />
                  {days}<em>天</em>
                  {alert ? <span className={styles.tagSfx}>· 需跟进</span> : null}
                </span>
              )
            },
          },
          {
            name: 'alertStatus',
            label: '预警状态',
            type: 'select',
            width: 120,
            options: [
              { label: '正常', value: 0 },
              { label: '60天未复购', value: 1 },
            ],
            render: (v) =>
              Number(v) === 1 ? (
                <span className={`${styles.alertChip} ${styles.alerted}`}>60天未复购</span>
              ) : (
                <span className={`${styles.alertChip} ${styles.normal}`}>正常</span>
              ),
          },
          { name: 'remark', label: '备注', type: 'textarea', ellipsis: true },
        ]}
      />
    </div>
  )
}
