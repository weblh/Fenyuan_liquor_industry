-- 修复客户监管「未复购天数」因 DEFAULT 0 全部显示为 0 的历史数据
-- 仅清理管家婆同步、且尚无真实购买日的记录
UPDATE biz_customer_maintain
SET days_since_purchase = NULL,
    alert_status = 0,
    last_purchase_date = NULL
WHERE remark LIKE '[GJP:%'
  AND (days_since_purchase = 0 OR days_since_purchase IS NULL)
  AND (last_purchase_date IS NULL OR last_purchase_date = '' OR last_purchase_date = '0');

-- 可选：将 days_since_purchase 默认值改为允许 NULL（MySQL）
-- ALTER TABLE biz_customer_maintain MODIFY COLUMN days_since_purchase INT NULL DEFAULT NULL COMMENT '未复购天数';
