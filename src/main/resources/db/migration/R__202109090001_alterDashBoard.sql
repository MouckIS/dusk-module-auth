alter table if exists dashboard_theme add column if not exists main_page bool;

--删除dashboard_premission 数据
delete from dashboard_premission;
alter table if exists dashboard_premission add column if not exists role_id int8;
alter table if exists dashboard_premission drop column if exists user_id;
