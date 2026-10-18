package com.dusk.module.auth.authz.version;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 角色权限版本服务：为一次权限变更分配一个单调递增的 {@code permissionVersion}（8.5）。
 *
 * <p>并发安全依赖行级悲观锁（{@link IAuthzRoleVersionRepository#findWithLockByRoleId}）：
 * 同一角色的并发权限变更被串行化，不会产生重复版本。首次插入的并发冲突由唯一索引兜底，
 * 冲突后降级为「重读 + 自增」，最多一次。</p>
 *
 * <p><b>事务传播</b>：默认加入调用方事务（{@code RoleServiceImpl} 是 {@code @Transactional}），
 * 因此「版本号自增」与「业务写入 + Outbox 写入」在同一个事务里提交，任一步失败整体回滚——
 * 这正是 Outbox 模式要求的原子性。</p>
 */
@Service
@Slf4j
public class AuthzPermissionVersionService {

    private final IAuthzRoleVersionRepository repository;

    public AuthzPermissionVersionService(IAuthzRoleVersionRepository repository) {
        this.repository = repository;
    }

    /**
     * 为角色分配下一个权限版本号。
     *
     * @param tenantId 租户 ID，可为空（仅记录用）
     * @param roleId   角色 ID，不可为空
     * @return 自增后的版本号（从 1 开始）
     */
    @Transactional
    public long nextVersion(Long tenantId, Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("分配 permissionVersion 失败：roleId 不能为空");
        }
        Optional<AuthzRoleVersion> existing = repository.findWithLockByRoleId(roleId);
        if (existing.isPresent()) {
            return bump(existing.get());
        }
        AuthzRoleVersion created = new AuthzRoleVersion();
        created.setRoleId(roleId);
        created.setTenantId(tenantId);
        created.setPermissionVersion(1L);
        try {
            repository.save(created);
            return 1L;
        } catch (DataIntegrityViolationException e) {
            // 并发首次插入撞唯一索引：另一个事务已建行，改为在其上自增。
            log.debug("角色权限版本首次插入冲突，降级为自增：roleId={}", roleId);
            AuthzRoleVersion row = repository.findWithLockByRoleId(roleId)
                    .orElseThrow(() -> new IllegalStateException("角色权限版本并发冲突后仍读不到行：roleId=" + roleId, e));
            return bump(row);
        }
    }

    private long bump(AuthzRoleVersion row) {
        long next = row.getPermissionVersion() + 1;
        row.setPermissionVersion(next);
        repository.save(row);
        return next;
    }
}
