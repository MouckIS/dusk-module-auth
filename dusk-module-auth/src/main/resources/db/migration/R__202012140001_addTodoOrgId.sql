alter table if exists  sys_todos add column org_id int8;
create index index_todos_orgid on sys_todos(org_id);