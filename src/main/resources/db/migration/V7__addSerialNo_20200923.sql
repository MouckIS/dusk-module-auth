-- 添加流水号表
create table sys_serial_no (id int8 not null, create_id int8, create_time timestamp, dr int4, last_modify_id int8, last_modify_time timestamp, tenant_id int8, version int4, bill_type varchar(255), current_no int8 not null, date_format varchar(255), last_no varchar(255), last_update_time timestamp, no_length int4 not null, reset_type varchar(255), primary key (id));

--添加索引
create index index_no_tenant_type on sys_serial_no(tenant_id,bill_type);