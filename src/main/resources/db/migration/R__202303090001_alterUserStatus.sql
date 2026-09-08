update sys_user set user_status = 'ON_JOB' where sys_user.user_status is null;
update sys_user set user_type = 'INNER' where sys_user.user_type is null;
update sys_organization set type = 'INNER' where sys_organization.type is null;