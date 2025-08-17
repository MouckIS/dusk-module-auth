create table if not exists sys_data_display_set
(
    id                          int8 not null,
    create_id                   int8,
    create_time                 timestamp,
    last_modify_id              int8,
    last_modify_time            timestamp,
    version                     int4,
    dr                          int4,
    tenant_id                   int8,
    display_type                varchar(255),
    primary key (id)
);

