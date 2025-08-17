-- 创建用户登入登出日志表
create table if not exists sys_user_login_log
(
    id               int8 not null,
    tenant_id        int8,
    user_name        varchar(255),
    log_type         varchar(30),
    operation_time   timestamp,
    ip               varchar(255),
    browser_info     varchar(1024),
    success          bool,
    msg              varchar(1024),
    primary key (id)
);