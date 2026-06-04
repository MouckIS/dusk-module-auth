-- 数据维度表
CREATE TABLE IF NOT EXISTS sys_data_dimension
(
    id              BIGINT       NOT NULL PRIMARY KEY,
    dimension_name  VARCHAR(100) NOT NULL,
    dimension_code  VARCHAR(100) NOT NULL,
    dimension_desc  VARCHAR(500),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    create_id       BIGINT,
    create_time     TIMESTAMP,
    last_modify_id  BIGINT,
    last_modify_time TIMESTAMP,
    version         INT          NOT NULL DEFAULT 0,
    dr              INT          NOT NULL DEFAULT 0,
    tenant_id       BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dimension_code_tenant ON sys_data_dimension (dimension_code, tenant_id);

COMMENT ON TABLE sys_data_dimension IS '数据维度表';
COMMENT ON COLUMN sys_data_dimension.dimension_name IS '维度名称';
COMMENT ON COLUMN sys_data_dimension.dimension_code IS '维度编码';
COMMENT ON COLUMN sys_data_dimension.dimension_desc IS '维度描述';
COMMENT ON COLUMN sys_data_dimension.enabled IS '是否启用';

-- 维度值表
CREATE TABLE IF NOT EXISTS sys_dimension_value
(
    id              BIGINT       NOT NULL PRIMARY KEY,
    dimension_id    BIGINT       NOT NULL,
    value_code      VARCHAR(100) NOT NULL,
    value_name      VARCHAR(200) NOT NULL,
    value_desc      VARCHAR(500),
    sort_index      INT,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    create_id       BIGINT,
    create_time     TIMESTAMP,
    last_modify_id  BIGINT,
    last_modify_time TIMESTAMP,
    version         INT          NOT NULL DEFAULT 0,
    dr              INT          NOT NULL DEFAULT 0,
    tenant_id       BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dim_value_code_tenant ON sys_dimension_value (dimension_id, value_code, tenant_id);
CREATE INDEX IF NOT EXISTS idx_dim_value_dimension_id ON sys_dimension_value (dimension_id);

COMMENT ON TABLE sys_dimension_value IS '维度值表';
COMMENT ON COLUMN sys_dimension_value.dimension_id IS '所属维度ID';
COMMENT ON COLUMN sys_dimension_value.value_code IS '维度值编码';
COMMENT ON COLUMN sys_dimension_value.value_name IS '维度值名称';
COMMENT ON COLUMN sys_dimension_value.value_desc IS '维度值描述';
COMMENT ON COLUMN sys_dimension_value.sort_index IS '排序号';
COMMENT ON COLUMN sys_dimension_value.enabled IS '是否启用';

-- 用户维度值权限表
CREATE TABLE IF NOT EXISTS sys_user_dimension_permission
(
    id                  BIGINT NOT NULL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    dimension_id        BIGINT NOT NULL,
    dimension_value_id  BIGINT NOT NULL,
    create_id           BIGINT,
    create_time         TIMESTAMP,
    last_modify_id      BIGINT,
    last_modify_time    TIMESTAMP,
    version             INT    NOT NULL DEFAULT 0,
    dr                  INT    NOT NULL DEFAULT 0,
    tenant_id           BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_dim_value_tenant ON sys_user_dimension_permission (user_id, dimension_value_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_dim_perm_user_id ON sys_user_dimension_permission (user_id);
CREATE INDEX IF NOT EXISTS idx_user_dim_perm_dim_id ON sys_user_dimension_permission (dimension_id);
CREATE INDEX IF NOT EXISTS idx_user_dim_perm_value_id ON sys_user_dimension_permission (dimension_value_id);

COMMENT ON TABLE sys_user_dimension_permission IS '用户维度值权限表';
COMMENT ON COLUMN sys_user_dimension_permission.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_dimension_permission.dimension_id IS '维度ID';
COMMENT ON COLUMN sys_user_dimension_permission.dimension_value_id IS '维度值ID';

-- 维度操作日志表
CREATE TABLE IF NOT EXISTS sys_dimension_operation_log
(
    id                BIGINT      NOT NULL PRIMARY KEY,
    operation_type    INT         NOT NULL,
    target_type       VARCHAR(50) NOT NULL,
    target_id         BIGINT,
    target_name       VARCHAR(200),
    operation_detail  VARCHAR(2000),
    operator_id       BIGINT,
    operator_name     VARCHAR(100),
    tenant_id         BIGINT,
    client_ip         VARCHAR(50),
    create_id         BIGINT,
    create_time       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dim_op_log_operator ON sys_dimension_operation_log (operator_id);
CREATE INDEX IF NOT EXISTS idx_dim_op_log_type ON sys_dimension_operation_log (operation_type);
CREATE INDEX IF NOT EXISTS idx_dim_op_log_target ON sys_dimension_operation_log (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_dim_op_log_time ON sys_dimension_operation_log (create_time);

COMMENT ON TABLE sys_dimension_operation_log IS '维度操作日志表';
COMMENT ON COLUMN sys_dimension_operation_log.operation_type IS '操作类型（0新增 1修改 2删除 3导入 4导出 5授权 6撤销授权 7批量授权 8批量撤销授权）';
COMMENT ON COLUMN sys_dimension_operation_log.target_type IS '操作目标类型（DIMENSION/DIMENSION_VALUE/PERMISSION）';
COMMENT ON COLUMN sys_dimension_operation_log.target_id IS '操作目标ID';
COMMENT ON COLUMN sys_dimension_operation_log.target_name IS '操作目标名称';
COMMENT ON COLUMN sys_dimension_operation_log.operation_detail IS '操作详情';
COMMENT ON COLUMN sys_dimension_operation_log.operator_id IS '操作用户ID';
COMMENT ON COLUMN sys_dimension_operation_log.operator_name IS '操作用户名称';
COMMENT ON COLUMN sys_dimension_operation_log.client_ip IS '客户端IP';
