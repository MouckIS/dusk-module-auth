alter table if exists sys_permissions add column if not exists business_key varchar(255);
create index index_sys_permissions_business_key on sys_permissions (business_key);