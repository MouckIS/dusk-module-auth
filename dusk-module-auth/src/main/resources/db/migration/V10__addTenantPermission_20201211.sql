create table sys_tenant_permissions (id int8 not null, create_id int8, create_time timestamp, edition_id int8,  name varchar(255),  primary key (id));

create index index_tenant_permissions_editionid on sys_tenant_permissions(edition_id);