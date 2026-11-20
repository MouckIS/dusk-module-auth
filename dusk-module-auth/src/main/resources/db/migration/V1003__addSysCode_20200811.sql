-- 创建系统常量类型表以及常量表
create table sys_code_values
(
    id               int8 not null,
    create_id        int8,
    create_time      timestamp,
    dr               int4,
    last_modify_id   int8,
    last_modify_time timestamp,
    tenant_id        int8,
    version          int4,
    code             varchar(255),
    type_code        varchar(255),
    describe         varchar(255),
    enabled          boolean,
    parent_id        int8,
    sort_index       int4 not null,
    value            varchar(255),
    primary key (id)
);


create index index_sysCodeValue_parentId on sys_code_values (parent_id);
create index index_sysCodeValue_typeCode on sys_code_values (type_code);
create index index_sysCodeValue_code on sys_code_values (code);