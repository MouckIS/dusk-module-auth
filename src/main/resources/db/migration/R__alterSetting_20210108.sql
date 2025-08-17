alter table if exists sys_setting add column if not exists station_id int8;

-- 自定义菜单增加是否忽略属性
alter table if exists sys_menu add column if not exists ignored bool;