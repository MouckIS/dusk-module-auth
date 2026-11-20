create table if not exists user_wx_relation
(
    id               int8 not null,
    create_id        int8,
    create_time      timestamp,
    dr               int4,
    last_modify_id   int8,
    last_modify_time timestamp,
    tenant_id        int8,
    version          int4,
    app_id           varchar(255),
    open_id          varchar(255),
    user_id          int8,
    primary key (id)
);

create index if not exists userWxRelationUserId on user_wx_relation (user_id);
create index if not exists userWxRelationAppId on user_wx_relation (app_id);