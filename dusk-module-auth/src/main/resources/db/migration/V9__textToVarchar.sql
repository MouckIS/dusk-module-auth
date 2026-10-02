alter table if exists sys_audit_logs drop column exception;
alter table if exists sys_audit_logs add column exception varchar(10485760);

alter table if exists sys_audit_logs drop column parameters;
alter table if exists sys_audit_logs add column parameters varchar(10485760);
