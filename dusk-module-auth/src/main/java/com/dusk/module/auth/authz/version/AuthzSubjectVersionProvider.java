package com.dusk.module.auth.authz.version;

import com.dusk.common.core.auth.registry.AuthzScope;
import com.dusk.common.core.jwt.SubjectVersionProvider;
import com.dusk.common.core.model.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 签发 JWT 时补充 {@code subjectVersion}，见《权限优化方案-整理版》5.3。
 *
 * <p>没有代际记录时返回 0：这表示账号从未发生过「改角色/禁用/吊销」；
 * 一旦发生，版本表与 JWT 都会推进，旧令牌会被网关与业务服务拒绝。</p>
 */
@Component
@Slf4j
public class AuthzSubjectVersionProvider implements SubjectVersionProvider {

    private final AuthzSubjectVersionService versionService;

    public AuthzSubjectVersionProvider(AuthzSubjectVersionService versionService) {
        this.versionService = versionService;
    }

    @Override
    public Long currentSubjectVersion(UserContext user) {
        if (user == null || user.getId() == null) {
            return 0L;
        }
        try {
            return versionService.currentVersion(AuthzScope.of(user.getTenantId()), user.getId());
        } catch (Exception e) {
            // 版本表尚未迁移/数据库瞬时不可用时，登录不能被阻断；签发 0 只会让后续校验
            // 在首次改角色时要求重新登录，不会扩大权限。
            log.warn("读取用户代际失败，JWT 将使用 0：userId={}, reason={}", user.getId(), e.getMessage());
            return 0L;
        }
    }
}
