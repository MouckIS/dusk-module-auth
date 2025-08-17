alter table if exists sys_notification add column page_navigation text;
alter table if exists sys_notification add column type int4;
alter table if exists sys_user_notification drop column is_read;
alter table if exists sys_user_notification add column read boolean;