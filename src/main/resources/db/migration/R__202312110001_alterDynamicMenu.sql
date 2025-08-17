alter table if exists sys_dynamic_menu add column if not exists type varchar(20);
update sys_dynamic_menu set type = 'INNER' where sys_dynamic_menu.type is null;