create index index_syslog_tenantid on sys_audit_logs (tenant_id);
create index index_permission_tenantid on sys_permissions(tenant_id);
create index index_feature_tenantid on sys_feature_value(tenant_id);
create index index_todo_tenantid on sys_todos(tenant_id);
create index index_user_tenantid on sys_user(tenant_id);
create index index_role_tenantid on sys_role(tenant_id);
create index index_sysCodeValue_tenantid on sys_code_values (tenant_id);
create index index_org_tenantid on sys_organization(tenant_id);