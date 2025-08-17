create table if not exists sys_org_manager
(
    id               int8 not null,
    create_id        int8,
    create_time      timestamp,
    last_modify_id   int8,
    last_modify_time timestamp,
    version          int4,
    dr               int4,
    tenant_id        int8,
    user_id          int8,
    org_id           int8,
    primary key (id)
);