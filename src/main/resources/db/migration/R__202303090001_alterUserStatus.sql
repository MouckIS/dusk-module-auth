update sys_user set user_status = 'OnJob' where sys_user.user_status is null;
update sys_user set user_type = 'Inner' where sys_user.user_type is null;
update sys_organization set type = 'Inner' where sys_organization.type is null;