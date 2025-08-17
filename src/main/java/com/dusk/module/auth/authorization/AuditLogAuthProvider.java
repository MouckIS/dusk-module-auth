package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-21 11:16
 */
@Component
public class AuditLogAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION_AUDITLOGS = "Pages.Administration.AuditLogs";
    public static final String PAGES_ADMINISTRATION_AUDITLOGS_EXPORT = "Pages.Administration.AuditLogs.Export";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission auditLog = administration.createChildPermission(PAGES_ADMINISTRATION_AUDITLOGS, "审计日志");
        auditLog.createChildPermission(PAGES_ADMINISTRATION_AUDITLOGS_EXPORT, "导出日志");
    }
}
