package com.dusk.module.auth;

import com.dusk.common.core.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * 单元测试基础类 - 提供统一的测试环境初始化
 *
 * @author kefuming
 * @date 2026-02-28
 */
public abstract class BaseUnitTest {

    /**
     * 测试前初始化：清除租户上下文
     */
    @BeforeEach
    public void setUpBase() {
        TenantContextHolder.clear();
    }

    /**
     * 测试后清理：清除租户上下文
     */
    @AfterEach
    public void tearDownBase() {
        TenantContextHolder.clear();
    }

    /**
     * 设置租户ID
     */
    protected void setTenantId(Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
    }

    /**
     * 获取租户ID
     */
    protected Long getTenantId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * 清除租户上下文
     */
    protected void clearTenantContext() {
        TenantContextHolder.clear();
    }
}

