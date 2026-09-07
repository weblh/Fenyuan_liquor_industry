-- 拆分金蝶 / 管家婆职责：业务同步入口独立；金蝶仅写凭证
-- mysql -uroot -p123456 Fenyuan_liquor_industry < backend/sql/update_cmcloud_menu_20260828.sql

USE Fenyuan_liquor_industry;

UPDATE sys_menu
SET name = '凭证记录'
WHERE id = 64 AND path = '/finance/kingdee-voucher';

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 80, 26, '管家婆同步', '/business/cmcloud-sync', 'business/cmcloudSync/index', 'business:cmcloud:sync', 1, 'CloudSyncOutlined', 9, 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 80);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 80 FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_role WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 80);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 80
FROM sys_role_menu rm
WHERE rm.menu_id IN (26, 56, 29)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 80
  );
