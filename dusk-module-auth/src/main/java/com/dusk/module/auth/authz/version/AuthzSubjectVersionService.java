package com.dusk.module.auth.authz.version;

import com.dusk.common.core.auth.registry.AuthzScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户代际版本服务：为一次账号/角色变更分配单调递增的 {@code subjectVersion}（5.3）。
 *
 * <p>与角色权限版本一样依赖行级悲观锁串行化；首次插入冲突后降级为重读 + 自增。</p>
 */
@Service
@Slf4j
public class AuthzSubjectVersionService {

    private final IAuthzSubjectVersionRepository repository;

    public AuthzSubjectVersionService(IAuthzSubjectVersionRepository repository) {
        this.repository = repository;
    }

    /**
     * 为指定作用域下的用户分配下一个代际版本。
     */
    @Transactional
    public long nextVersion(String tenantScope, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("分配 subjectVersion 失败：userId 不能为空");
        }
        String scope = AuthzScope.require(tenantScope);
        Optional<AuthzSubjectVersion> existing = repository.findWithLockByScopeAndUserId(scope, userId);
        if (existing.isPresent()) {
            return bump(existing.get());
        }
        AuthzSubjectVersion created = new AuthzSubjectVersion();
        created.setTenantScope(scope);
        created.setUserId(userId);
        created.setSubjectVersion(1L);
        try {
            repository.save(created);
            return 1L;
        } catch (DataIntegrityViolationException e) {
            log.debug("用户代际版本首次插入冲突，降级为自增：scope={}, userId={}", scope, userId);
            AuthzSubjectVersion row = repository.findWithLockByScopeAndUserId(scope, userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "用户代际版本并发冲突后仍读不到行：scope=" + scope + ", userId=" + userId, e));
            return bump(row);
        }
    }

    /**
     * 读取当前代际；不存在时返回 {@code 0}（表示尚未发生过需要推进代际的变更）。
     */
    @Transactional(readOnly = true)
    public long currentVersion(String tenantScope, Long userId) {
        if (userId == null) {
            return 0L;
        }
        return repository.findByTenantScopeAndUserId(AuthzScope.require(tenantScope), userId)
                .map(AuthzSubjectVersion::getSubjectVersion)
                .orElse(0L);
    }

    private long bump(AuthzSubjectVersion row) {
        long next = row.getSubjectVersion() + 1;
        row.setSubjectVersion(next);
        repository.save(row);
        return next;
    }
}
