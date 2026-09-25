package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jwt.JwtTokenFactory;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.rpc.auth.dto.GenerateTokenForNonUserInput;
import com.dusk.common.rpc.auth.dto.RoleSimpleDto;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.service.IRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TokenAuthRpcService} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class TokenAuthRpcServiceTest {

    private static final String TOKEN_PREFIX = "CRUX:AUTH:NONUSER:TOKEN:";

    @Mock
    private RedisUtil<String> redisUtil;
    @Mock
    private JwtTokenFactory jwtTokenFactory;
    @Mock
    private IRoleService roleService;
    @Mock
    private TokenAuthManager tokenAuthManager;

    private TokenAuthRpcService service;

    @BeforeEach
    void setUp() {
        service = new TokenAuthRpcService();
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(service, "jwtTokenFactory", jwtTokenFactory);
        ReflectionTestUtils.setField(service, "roleService", roleService);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);
    }

    private static GenerateTokenForNonUserInput input(String identify, List<String> roles) {
        GenerateTokenForNonUserInput input = new GenerateTokenForNonUserInput();
        input.setIdentify(identify);
        input.setRoles(roles);
        input.setTime(10L);
        input.setUnit(TimeUnit.DAYS);
        return input;
    }

    private static RoleSimpleDto role(long id) {
        RoleSimpleDto dto = new RoleSimpleDto();
        dto.setId(id);
        return dto;
    }

    @Test
    @DisplayName("generateTokenForNonUser：identify 为空时抛出业务异常")
    void generateTokenRejectsBlankIdentify() {
        assertThatThrownBy(() -> service.generateTokenForNonUser(input("  ", List.of("r"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("generateTokenForNonUser：角色为空时抛出业务异常")
    void generateTokenRejectsEmptyRoles() {
        when(roleService.getByRoleNames(List.of("r"))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.generateTokenForNonUser(input("ident", List.of("r"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("generateTokenForNonUser：正常签发并缓存 token")
    void generateTokenStoresAndReturnsJwtId() {
        when(roleService.getByRoleNames(List.of("r"))).thenReturn(List.of(role(11L)));
        when(redisUtil.hasKey(TOKEN_PREFIX + "ident")).thenReturn(false);
        when(tokenAuthManager.generateToken(any(UserContext.class), eq(10L), eq(TimeUnit.DAYS))).thenReturn("raw-token");
        when(jwtTokenFactory.getJwtTokenId("raw-token")).thenReturn("jwt-id");

        String result = service.generateTokenForNonUser(input("ident", List.of("r")));

        assertThat(result).isEqualTo("jwt-id");
        verify(redisUtil).setCache(TOKEN_PREFIX + "ident", "raw-token", 10L, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("removeToken：存在缓存时移除 token 并删除缓存")
    void removeTokenDeletesWhenKeyExists() {
        when(redisUtil.hasKey(TOKEN_PREFIX + "ident")).thenReturn(true);
        when(redisUtil.getCache(TOKEN_PREFIX + "ident")).thenReturn("cached-token");

        service.removeToken("ident");

        verify(tokenAuthManager).removeToken(any(String.class));
        verify(redisUtil).deleteCache(TOKEN_PREFIX + "ident");
    }

    @Test
    @DisplayName("removeToken：不存在缓存时不做任何操作")
    void removeTokenSkipsWhenKeyMissing() {
        when(redisUtil.hasKey(TOKEN_PREFIX + "ident")).thenReturn(false);

        service.removeToken("ident");

        verify(redisUtil, never()).deleteCache(TOKEN_PREFIX + "ident");
        verify(tokenAuthManager, never()).removeToken(any(String.class));
    }
}
