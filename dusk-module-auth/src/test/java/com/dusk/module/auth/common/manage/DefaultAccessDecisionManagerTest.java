package com.dusk.module.auth.common.manage;

import com.dusk.common.core.model.UserContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultAccessDecisionManager} 单元测试，目标：100% 分支覆盖。
 */
class DefaultAccessDecisionManagerTest {

    private final DefaultAccessDecisionManager manager = new DefaultAccessDecisionManager();

    private static UserContext userWithRoles(String... roles) {
        UserContext userContext = new UserContext();
        userContext.setAuthorities(List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList());
        return userContext;
    }

    @Test
    @DisplayName("decide：无权限要求时直接放行")
    void decide_nullAttributes() {
        assertThat(manager.decide(userWithRoles(), null)).isTrue();
    }

    @Test
    @DisplayName("decide：权限要求为空集合时直接放行")
    void decide_emptyAttributes() {
        assertThat(manager.decide(userWithRoles(), List.of())).isTrue();
    }

    @Test
    @DisplayName("decide：用户拥有任一所需权限时放行")
    void decide_matched() {
        assertThat(manager.decide(userWithRoles("ROLE_USER", "ROLE_ADMIN"),
                List.of("ROLE_ADMIN", "ROLE_SUPER"))).isTrue();
    }

    @Test
    @DisplayName("decide：用户没有任何所需权限时拒绝")
    void decide_notMatched() {
        assertThat(manager.decide(userWithRoles("ROLE_USER"),
                List.of("ROLE_ADMIN", "ROLE_SUPER"))).isFalse();
    }

    @Test
    @DisplayName("decide：用户无任何权限时拒绝")
    void decide_emptyAuthorities() {
        assertThat(manager.decide(userWithRoles(), List.of("ROLE_ADMIN"))).isFalse();
    }
}
