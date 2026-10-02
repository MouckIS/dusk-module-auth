create table if not exists sys_page_quick_entry
(
    id                          int8 not null,
    create_id                   int8,
    create_time                 timestamp,
    last_modify_id              int8,
    last_modify_time            timestamp,
    version                     int4,
    dr                          int4,
    tenant_id                   int8,
    route_name                varchar(255),
    primary key (id)
);
