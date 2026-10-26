package com.dusk.module.auth.registry.service;

import com.dusk.common.core.exception.BusinessException;

/**
 * 快照引用了权限目录中不存在的权限码，见《权限优化方案-整理版》3.1。
 *
 * <p>确定性失败：调用方不得重试；修复权限树或代码后重新注册。</p>
 */
public class UnknownPermissionException extends BusinessException {

    private final String permissionCode;

    public UnknownPermissionException(String permissionCode) {
        super("Resource Snapshot 引用了权限目录中不存在的权限码：" + permissionCode
                + "。权限只能由 Auth 的权限树（*AuthProvider）定义，SDK 不得自动创建业务权限。");
        this.permissionCode = permissionCode;
    }

    public String permissionCode() {
        return permissionCode;
    }
}
