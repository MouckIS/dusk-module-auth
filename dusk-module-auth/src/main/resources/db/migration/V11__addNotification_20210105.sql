create table if not exists sys_notification
(
    id               int8 not null,
    create_id        int8,
    create_time      timestamp,
    dr               int4,
    last_modify_id   int8,
    last_modify_time timestamp,
    tenant_id        int8,
    version          int4,
    content          varchar(255),
    title            varchar(255),
    primary key (id)
);

create table if not exists sys_user_notification
(
    id               int8    not null,
    create_id        int8,
    create_time      timestamp,
    dr               int4,
    last_modify_id   int8,
    last_modify_time timestamp,
    tenant_id        int8,
    version          int4,
    is_read          boolean not null,
    notification_id  int8,
    user_id          int8,
    primary key (id)
);

create index if not exists idx_sys_notification_tenant_id on sys_notification (tenant_id);
create index if not exists idx_sys_user_notification_tenant_id on sys_user_notification (tenant_id);
alter table if exists sys_user_notification
    add constraint FK_User_Notification foreign key (notification_id) references sys_notification;
