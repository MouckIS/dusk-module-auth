create table if not exists user_fingerprint
(
    id               int8 not null,
    create_id        int8,
    create_time      timestamp,
    dr               int4,
    last_modify_id   int8,
    last_modify_time timestamp,
    tenant_id        int8,
    version          int4,
    data             varchar(1048576),
    from_enum        varchar(255),
    name             varchar(255),
    size             int4,
    user_id          int8,
    user_seq         int4,
    primary key (id)
);

create index if not exists userFingerprintUserId on user_fingerprint (user_id);



