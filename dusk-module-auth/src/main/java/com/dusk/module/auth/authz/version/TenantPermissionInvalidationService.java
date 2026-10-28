package com.dusk.module.auth.authz.version;

import com.dusk.module.auth.authz.service.AuthzEventPublisher;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 套餐权限变更后的角色失效，见《权限优化方案-整理版》3.3。
 *
 * <p>TenantPermission 决定了租户内角色权限的上界。修改套餐授权后，所有使用该套餐的租户下
 * 所有角色的有效权限都可能变化，因此必须逐个推进 {@code permissionVersion} 并发事件，
 * 否则网关会继续使用旧的角色权限缓存直到 TTL 到期。</p>
 */
@Service
@Slf4j
public class TenantPermissionInvalidationService {

    private final ITenantRepository tenantRepository;
    private final IRoleRepository roleRepository;
    private final AuthzPermissionVersionService permissionVersionService;
    private final AuthzEventPublisher eventPublisher;

    public TenantPermissionInvalidationService(ITenantRepository tenantRepository,
                                               IRoleRepository roleRepository,
                                               AuthzPermissionVersionService permissionVersionService,
                                               AuthzEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.permissionVersionService = permissionVersionService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 使指定套餐下所有租户、所有角色失效。
     *
     * @return 推进版本的角色数
     */
    @Transactional
    public int invalidateEdition(Long editionId) {
        if (editionId == null) {
            return 0;
        }
        int affected = 0;
        for (var tenant : tenantRepository.findByEdition_Id(editionId)) {
            Long tenantId = tenant.getId();
            for (Role role : roleRepository.findByTenantId(tenantId)) {
                long version = permissionVersionService.nextVersion(tenantId, role.getId());
                eventPublisher.permissionChanged(tenantId, role.getId(), version);
                affected++;
            }
        }
        log.info("套餐权限变更，已失效角色权限：editionId={}, roles={}", editionId, affected);
        return affected;
    }
}
