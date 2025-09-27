package com.dusk.module.ddm.module.auth.service;

import com.dusk.module.auth.dto.tenant.*;
import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dto.tenant.*;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.repository.ITenantRepository;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-04-30 10:14
 */
public interface ITenantService extends IBaseService<Tenant, ITenantRepository> {
    /**
     * 创建租户并初始化默认设置
     * @param input
     * @return
     */
    Tenant createTenantWithDefaultSettings(CreateTenantInput input);

    /**
     * 更新租户
     * @param tenantEditDto
     */
    void updateTenant(TenantEditDto tenantEditDto);

    /**
     * 查询租户（分页）
     * @param input
     * @return
     */
    Page<Tenant> getTenants(GetTenantsInput input);

    /**
     * 根据租户代码查找租户
     * @param name
     * @return
     */
    Optional<Tenant> findByTenantName(String name);

    /**
     * 租户是否可用
     * @param input
     * @return
     */
    IsTenantAvailableOutput isTenantAvailable(IsTenantAvailableInput input);

    /**
     * 计数有多少租户关联到版本
     * @param editionId
     * @return
     */
    long countTenantsByEdition(Long editionId);

    /**
     * 删除租户
     * @param id
     * @return
     */
    void deleteTenant(Long id);
}
