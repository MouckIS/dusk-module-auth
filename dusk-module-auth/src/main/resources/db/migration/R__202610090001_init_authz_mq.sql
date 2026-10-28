-- =============================================================================
-- 权限事件推送（MQ）表结构
-- 对应方案：《权限优化方案-整理版》第八章「MQ 权限推送实现」
--   - sys_authz_outbox_event : Transactional Outbox（8.5）
--   - sys_authz_role_version : 角色权限版本号（8.5，permissionVersion 的单调来源）
--
-- 设计红线：
--   ⑦ RocketMQ 不是 Source of Truth；事件必须先落库、再投递（Outbox）
--   ⑧ PostgreSQL 是 Auth 的 Source of Truth
--   ⑪ 事件按租户作用域（PERMISSION_CHANGED 的 tenantId 在载荷/本表冗余列中）
--
-- 注意：本脚本只建新表，不改动既有表。repeatable 迁移要求可重复执行。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Outbox 事件表
--    与业务写入同事务提交；投递状态由 AuthzOutboxPublisher 维护。
--    status: PENDING / SENT / FAILED（FAILED 保留待人工重放，绝不丢弃）
-- -----------------------------------------------------------------------------
create table if not exists sys_authz_outbox_event (
    id            int8         not null,
    create_id     int8,
    create_time   timestamp,
    event_type    varchar(64)  not null,
    biz_key       varchar(255),
    ref_key       varchar(255),
    payload       text         not null,
    status        varchar(16)  not null,
    attempts      int4         not null default 0,
    next_retry_at timestamp,
    last_error    varchar(1000),
    sent_at       timestamp,
    primary key (id)
);

-- 发布器按 (status, next_retry_at) 捞取到期事件，此索引直接服务该查询。
create index if not exists index_authz_outbox_status_retry
    on sys_authz_outbox_event (status, next_retry_at);

-- 业务键用于幂等排查与手工检索。
create index if not exists index_authz_outbox_biz_key
    on sys_authz_outbox_event (biz_key);

-- -----------------------------------------------------------------------------
-- 2. 角色权限版本号
--    permissionVersion 是单调递增整数，用于网关「仅当缓存版本更旧才失效」的判定。
--    必须持久化：进程重启后版本仍须可比，否则事件与缓存版本不可比。
--    宿主/租户角色共用一张表，故仅按 role_id 唯一，不按租户唯一。
-- -----------------------------------------------------------------------------
create table if not exists sys_authz_role_version (
    id                 int8        not null,
    create_id          int8,
    create_time        timestamp,
    role_id            int8        not null,
    tenant_id          int8,
    permission_version int8        not null,
    primary key (id)
);

create unique index if not exists uk_authz_role_version_role_id
    on sys_authz_role_version (role_id);
