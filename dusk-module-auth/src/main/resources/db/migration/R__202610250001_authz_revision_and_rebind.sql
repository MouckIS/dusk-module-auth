-- =============================================================================
-- 权限优化方案 2026-10-25 落地：不可变 Release、resourceRevision、REBIND 审批、
-- subjectVersion、Outbox 租约与投影状态。
--
-- 使用 repeatable 迁移的原因：权限注册表本身由 R__202610070001 / R__202610090001 创建，
-- 在全新数据库上 Flyway 会先执行所有版本化脚本、再执行 repeatable 脚本；
-- 若把这些 ALTER 放进 V 脚本，会在建表之前执行而失败。
--
-- 所有语句必须可重复执行（幂等）。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Service.resourceRevision：服务级单调修订号（3.2）
-- -----------------------------------------------------------------------------
alter table sys_registry_service
    add column if not exists resource_revision int8 not null default 0;

-- -----------------------------------------------------------------------------
-- 2. Release 不可变身份 (service_id, service_version, resource_version)（4.2）
-- -----------------------------------------------------------------------------
alter table sys_registry_release
    add column if not exists activation_blocked_reason varchar (1000);

do
$$
begin
    if
exists (select 1 from pg_constraint where conname = 'uk_registry_release_service_version') then
alter table sys_registry_release drop constraint uk_registry_release_service_version;
end if;
    if
not exists (select 1 from pg_constraint where conname = 'uk_registry_release_identity') then
alter table sys_registry_release
    add constraint uk_registry_release_identity
        unique (service_id, service_version, resource_version);
end if;
end $$;

create index if not exists index_registry_release_identity_lookup
    on sys_registry_release (service_id, service_version, resource_version);

-- -----------------------------------------------------------------------------
-- 3. 换绑审批待办（4.7 / 8.1）
-- -----------------------------------------------------------------------------
create table if not exists sys_registry_rebind_request
(
    id
    int8
    not
    null,
    create_id
    int8,
    create_time
    timestamp,
    service_id
    varchar
(
    128
) not null,
    candidate_release_id int8 not null,
    http_method varchar
(
    16
) not null,
    path varchar
(
    512
) not null,
    old_permission_code varchar
(
    255
) not null,
    new_permission_code varchar
(
    255
) not null,
    status varchar
(
    16
) not null,
    requested_at timestamp,
    decided_at timestamp,
    decided_by varchar
(
    128
),
    decision_reason varchar
(
    1000
),
    activation_blocked_reason varchar
(
    1000
),
    primary key
(
    id
)
    );

create unique index if not exists uk_registry_rebind_candidate_route
    on sys_registry_rebind_request (candidate_release_id, http_method, path, new_permission_code);

create index if not exists index_registry_rebind_status
    on sys_registry_rebind_request (status);

create index if not exists index_registry_rebind_service_status
    on sys_registry_rebind_request (service_id, status);

-- -----------------------------------------------------------------------------
-- 4. 角色权限版本：作用域 + 角色唯一（3.3 / 红线⑧）
-- -----------------------------------------------------------------------------
alter table sys_authz_role_version
    add column if not exists tenant_scope varchar (64);

update sys_authz_role_version
set tenant_scope = case
                       when tenant_id is null then 'HOST'
                       else 'TENANT:' || tenant_id
    end
where tenant_scope is null;

alter table sys_authz_role_version
    alter column tenant_scope set not null;

drop index if exists uk_authz_role_version_role_id;

create unique index if not exists uk_authz_role_version_scope_role
    on sys_authz_role_version (tenant_scope, role_id);

-- -----------------------------------------------------------------------------
-- 5. 用户代际版本（5.3 / 8.1）
-- -----------------------------------------------------------------------------
create table if not exists sys_authz_subject_version
(
    id
    int8
    not
    null,
    create_id
    int8,
    create_time
    timestamp,
    tenant_scope
    varchar
(
    64
) not null,
    user_id int8 not null,
    subject_version int8 not null,
    primary key
(
    id
)
    );

create unique index if not exists uk_authz_subject_version_scope_user
    on sys_authz_subject_version (tenant_scope, user_id);

-- -----------------------------------------------------------------------------
-- 6. Outbox：版本、投影状态与数据库租约（6.2 / 6.3）
-- -----------------------------------------------------------------------------
alter table sys_authz_outbox_event
    add column if not exists revision int8 not null default 0;

alter table sys_authz_outbox_event
    add column if not exists projection_status varchar (16) not null default 'PENDING';

alter table sys_authz_outbox_event
    add column if not exists projected_at timestamp;

alter table sys_authz_outbox_event
    add column if not exists lease_owner varchar (128);

alter table sys_authz_outbox_event
    add column if not exists lease_until timestamp;

create index if not exists index_authz_outbox_claim
    on sys_authz_outbox_event (status, next_retry_at, lease_until);
