create table if not exists sys_menu
(
    id               int8 not null,
    create_id        int8,
    create_time      timestamp,
    dr               int4,
    last_modify_id   int8,
    last_modify_time timestamp,
    tenant_id        int8,
    version          int4,
    title            varchar(255),
    route_name       varchar(255),
    icon_type        varchar(255),
    icon_class       varchar(255),
    sort_index       int4,
    parent_route_name varchar (255),
    primary key (id)
);

