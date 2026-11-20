-- 添加租户描述
ALTER TABLE IF EXISTS sys_tenant ADD COLUMN description VARCHAR(255);
