package com.dusk.module.auth.common.skiprequest;

import com.dusk.module.auth.common.permission.IPermissionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link SkipPathRequestMatcher} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkipPathRequestMatcherTest {

    @Mock
    private IPermissionCache permissionCache;

    private SkipPathRequestMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new SkipPathRequestMatcher();
        ReflectionTestUtils.setField(matcher, "permissionCache", permissionCache);
    }

    @Test
    @DisplayName("matches：应用没有任何匿名路径时返回 false")
    void noAnonymousPathConfigured() {
        when(permissionCache.getAllowAnonymousPath()).thenReturn(Map.of());

        assertThat(matcher.matches("app", "/login")).isFalse();
    }

    @Test
    @DisplayName("matches：匿名路径列表为 null 时返回 false")
    void nullList() {
        Map<String, List<String>> paths = new HashMap<>();
        paths.put("app", null);
        when(permissionCache.getAllowAnonymousPath()).thenReturn(paths);

        assertThat(matcher.matches("app", "/login")).isFalse();
    }

    @Test
    @DisplayName("matches：匿名路径列表为空时返回 false")
    void emptyList() {
        when(permissionCache.getAllowAnonymousPath()).thenReturn(Map.of("app", List.of()));

        assertThat(matcher.matches("app", "/login")).isFalse();
    }

    @Test
    @DisplayName("matches：URL 命中匿名路径时返回 true")
    void matched() {
        when(permissionCache.getAllowAnonymousPath()).thenReturn(Map.of("app", List.of("/login", "/captcha/**")));

        assertThat(matcher.matches("app", "/captcha/image")).isTrue();
    }

    @Test
    @DisplayName("matches：URL 未命中任何匿名路径时返回 false")
    void notMatched() {
        when(permissionCache.getAllowAnonymousPath()).thenReturn(Map.of("app", List.of("/login", "/captcha/**")));

        assertThat(matcher.matches("app", "/api/user")).isFalse();
    }

    @Test
    @DisplayName("matches：命中列表中的第二个模式时返回 true")
    void matchedSecondPattern() {
        when(permissionCache.getAllowAnonymousPath())
                .thenReturn(Map.of("app", List.of("/first/**", "/second/**")));

        assertThat(matcher.matches("app", "/second/x")).isTrue();
    }
}
