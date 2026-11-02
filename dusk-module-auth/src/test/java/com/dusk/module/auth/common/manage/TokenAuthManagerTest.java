package com.dusk.module.auth.common.manage;

import com.dusk.common.core.jpa.Sequence;
import com.dusk.common.core.jwt.JwtSettings;
import com.dusk.common.core.jwt.JwtTokenFactory;
import com.dusk.common.core.jwt.extractor.JwtHeaderTokenExtractor;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.utils.UserContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link TokenAuthManager} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenAuthManagerTest {

    @Mock
    private RedisUtil<String> redisUtil;
    @Mock
    private JwtTokenFactory jwtTokenFactory;
    @Mock
    private JwtSettings jwtSettings;
    @Mock
    private UserContextUtils userContextUtils;
    @Mock
    private Sequence sequence;
    @Mock
    private JwtHeaderTokenExtractor jwtHeaderTokenExtractor;

    private TokenAuthManager manager;

    @BeforeEach
    void setUp() {
        manager = new TokenAuthManager();
        ReflectionTestUtils.setField(manager, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(manager, "jwtTokenFactory", jwtTokenFactory);
        ReflectionTestUtils.setField(manager, "jwtSettings", jwtSettings);
        ReflectionTestUtils.setField(manager, "userContextUtils", userContextUtils);
        ReflectionTestUtils.setField(manager, "sequence", sequence);
        ReflectionTestUtils.setField(manager, "jwtHeaderTokenExtractor", jwtHeaderTokenExtractor);
        when(jwtSettings.getExpirationTime()).thenReturn(30);
        when(sequence.nextId()).thenReturn(42L);
        when(jwtTokenFactory.createJwtToken(any(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn("jwt-token");
    }

    @Test
    @DisplayName("generateToken：默认使用配置的过期时间并写入缓存")
    void generateToken_defaultExpiration() {
        UserContext userContext = new UserContext();

        String token = manager.generateToken(userContext);

        assertThat(token).isEqualTo("jwt-token");
        verify(jwtTokenFactory).createJwtToken(userContext, "42", 30, TimeUnit.MINUTES);
        verify(redisUtil).setCache("CRUX:AUTH:TOKEN:42", "jwt-token", 30, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("generateToken：支持自定义有效期")
    void generateToken_customExpiration() {
        UserContext userContext = new UserContext();
        when(sequence.nextId()).thenReturn(99L);
        when(jwtTokenFactory.createJwtToken(userContext, "99", 5, TimeUnit.SECONDS)).thenReturn("short-token");

        String token = manager.generateToken(userContext, 5, TimeUnit.SECONDS);

        assertThat(token).isEqualTo("short-token");
        verify(redisUtil).setCache("CRUX:AUTH:TOKEN:99", "short-token", 5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("checkTokenValid：缓存中存在 token 时返回用户上下文")
    void checkTokenValid_validToken() {
        UserContext userContext = new UserContext();
        when(jwtHeaderTokenExtractor.extract("Bearer abc")).thenReturn("abc");
        when(jwtTokenFactory.getJwtTokenId("abc")).thenReturn("42");
        when(redisUtil.getCache("CRUX:AUTH:TOKEN:42")).thenReturn("jwt-token");
        when(userContextUtils.getUserContext("Bearer abc")).thenReturn(userContext);

        UserContext result = manager.checkTokenValid("Bearer abc");

        assertThat(result).isSameAs(userContext);
    }

    @Test
    @DisplayName("checkTokenValid：缓存为空时返回 null")
    void checkTokenValid_cacheMiss() {
        when(jwtHeaderTokenExtractor.extract("Bearer abc")).thenReturn("abc");
        when(jwtTokenFactory.getJwtTokenId("abc")).thenReturn("42");
        when(redisUtil.getCache("CRUX:AUTH:TOKEN:42")).thenReturn(null);

        assertThat(manager.checkTokenValid("Bearer abc")).isNull();
        verify(userContextUtils, never()).getUserContext(any(String.class));
    }

    @Test
    @DisplayName("checkTokenValid：解析异常时记录日志并返回 null")
    void checkTokenValid_exception() {
        when(jwtHeaderTokenExtractor.extract("Bearer bad")).thenThrow(new IllegalArgumentException("bad token"));

        assertThat(manager.checkTokenValid("Bearer bad")).isNull();
    }

    @Test
    @DisplayName("checkTokenValid(request)：从请求中提取 authorization 后复用字符串校验逻辑")
    void checkTokenValid_request() {
        UserContext userContext = new UserContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(userContextUtils.getAuthorization(request)).thenReturn("Bearer abc");
        when(jwtHeaderTokenExtractor.extract("Bearer abc")).thenReturn("abc");
        when(jwtTokenFactory.getJwtTokenId("abc")).thenReturn("42");
        when(redisUtil.getCache("CRUX:AUTH:TOKEN:42")).thenReturn("jwt-token");
        when(userContextUtils.getUserContext("Bearer abc")).thenReturn(userContext);

        assertThat(manager.checkTokenValid(request)).isSameAs(userContext);
    }

    @Test
    @DisplayName("removeToken：按 tokenId 删除缓存")
    void removeToken() {
        when(jwtHeaderTokenExtractor.extract("Bearer abc")).thenReturn("abc");
        when(jwtTokenFactory.getJwtTokenId("abc")).thenReturn("42");

        manager.removeToken("Bearer abc");

        verify(redisUtil).deleteCache("CRUX:AUTH:TOKEN:42");
    }

    @Test
    @DisplayName("getToken：按 tokenId 读取缓存")
    void getToken() {
        when(redisUtil.getCache("CRUX:AUTH:TOKEN:42")).thenReturn("jwt-token");

        assertThat(manager.getToken("42")).isEqualTo("jwt-token");
    }
}
