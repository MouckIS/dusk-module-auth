-- =============================================================================
-- 权限注册中心（Permission Registry）初始表结构
-- 对应方案：《权限优化方案-整理版》2.2 数据关系 / 3.2 Snapshot / 4.x 生命周期
--
-- 模型主线：Service -> Release -> Resource -> Permission
-- 设计红线：
--   ② API 删除 != Permission 删除（Resource 与 Permission 分表，生命周期解耦）
--   ④ Resource 必须绑定 Release（多版本并存）
--   ⑧ PostgreSQL 是本方案的 Source of Truth，Redis 只是可重建的投影
--
-- 注意：本脚本只建新表，不迁移存量数据。
--   存量 sys_permissions（宿主授权）/ sys_tenant_permissions（租户授权）
--   到新 RolePermission / TenantPermission 模型的迁移见 6.4 第 3 步，属后续改动。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 服务定义（Service）
-- -----------------------------------------------------------------------------
create table if not exists sys_registry_service (
    id                      int8         not null,
    create_id               int8,
    create_time             timestamp,
    service_id              varchar(128) not null,
    display_name            varchar(255),
    default_release_version varchar(64),
    primary key (id)
);

create unique index if not exists index_registry_service_service_id
    on sys_registry_service (service_id);

-- -----------------------------------------------------------------------------
-- 2. 服务发布版本（Release）
--    只要 Release 仍为 ACTIVE，其独有 Resource 对应的 Permission 就必须保持生效（4.5）
-- -----------------------------------------------------------------------------
create table if not exists sys_registry_release (
    id                  int8         not null,
    create_id           int8,
    create_time         timestamp,
    service_id          int8         not null,
    service_version     varchar(64)  not null,
    status              varchar(32)  not null,
    resource_version    varchar(128),
    zero_instance_since timestamp,
    last_synced_at      timestamp,
    activated_at        timestamp,
    retired_at          timestamp,
    primary key (id),
    constraint uk_registry_release_service_version unique (service_id, service_version),
    constraint fk_registry_release_service
        foreign key (service_id) references sys_registry_service (id)
);

create index if not exists index_registry_release_service_status
    on sys_registry_release (service_id, status);

create index if not exists index_registry_release_status
    on sys_registry_release (status);

-- -----------------------------------------------------------------------------
-- 3. API 资源（Resource）
--    (release_id, http_method, path) 精确唯一：红线⑫ 要求路径空间唯一，
--    运行时不依赖 Ant 模式的遍历顺序（5.10）。
--    permission_code 为「当前生效绑定」；pending_permission_code 为换绑候选（4.9）。
-- -----------------------------------------------------------------------------
create table if not exists sys_registry_resource (
    id                      int8         not null,
    create_id               int8,
    create_time             timestamp,
    release_id              int8         not null,
    resource_id             varchar(128),
    http_method             varchar(16)  not null,
    path                    varchar(512) not null,
    permission_code         varchar(255),
    pending_permission_code varchar(255),
    status                  varchar(32)  not null,
    status_changed_at       timestamp,
    primary key (id),
    constraint uk_registry_resource_release_method_path unique (release_id, http_method, path),
    constraint fk_registry_resource_release
        foreign key (release_id) references sys_registry_release (id)
);

create index if not exists index_registry_resource_permission_code
    on sys_registry_resource (permission_code);

create index if not exists index_registry_resource_pending_permission_code
    on sys_registry_resource (pending_permission_code);

create index if not exists index_registry_resource_release_status
    on sys_registry_resource (release_id, status);

-- -----------------------------------------------------------------------------
-- 4. 业务权限点（Permission）
--    ORPHANED 不等于删除：授权关系（RolePermission）始终保留，由管理员决定保留或禁用（4.3）
-- -----------------------------------------------------------------------------
create table if not exists sys_registry_permission (
    id                  int8         not null,
    create_id           int8,
    create_time         timestamp,
    code                varchar(255) not null,
    display_name        varchar(255),
    status              varchar(32)  not null,
    multi_tenancy_sides varchar(16),
    source_service_id   varchar(128),
    orphaned_at         timestamp,
    disabled_at         timestamp,
    primary key (id)
);

create unique index if not exists index_registry_permission_code
    on sys_registry_permission (code);

create index if not exists index_registry_permission_status
    on sys_registry_permission (status);
