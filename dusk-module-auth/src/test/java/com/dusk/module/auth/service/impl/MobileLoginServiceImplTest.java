package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.MobileAccountNotFoundException;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.mqs.pusher.PushSMS;
import com.dusk.common.mqs.pusher.SmsPushConfig;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.mobilelogin.SendCaptchaInput;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.feature.UserFeatureProvider;
import com.dusk.module.auth.push.INotificationPushManager;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ICaptchaService;
import com.dusk.module.auth.service.IFeatureService;
import com.dusk.module.auth.service.IUserService;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MobileLoginServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileLoginServiceImplTest {

    private static final long TENANT_ID = 77L;

    @Mock
    private RedisUtil<String> redisUtil;
    @Mock
    private TokenAuthManager tokenAuthManager;
    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private SmsPushConfig smsPushConfig;
    @Mock
    private INotificationPushManager pushManager;
    @Mock
    private IFeatureService featureService;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private IUserService userService;
    @Mock
    private ICaptchaService captchaService;

    private MobileLoginServiceImpl service;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        service = new MobileLoginServiceImpl();
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "smsPushConfig", smsPushConfig);
        ReflectionTestUtils.setField(service, "pushManager", pushManager);
        ReflectionTestUtils.setField(service, "featureService", featureService);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "captchaService", captchaService);

        request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubCountQuery(long count) {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.where(any(Predicate.class), any(Predicate.class))).thenReturn(query);
        when(query.fetchCount()).thenReturn(count);
    }

    private static User user(Long id, Long tenantId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setName("u" + id);
        user.setPhoneNo("13800000000");
        user.setUserRoles(new ArrayList<>());
        return user;
    }

    private static SendCaptchaInput captchaInput() {
        SendCaptchaInput input = new SendCaptchaInput();
        input.setMobile("13800000000");
        return input;
    }

    // ---------------- captcha ----------------

    @Test
    @DisplayName("captcha：推送服务未启用时抛出业务异常")
    void captchaThrowsWhenPushManagerDisabled() {
        ReflectionTestUtils.setField(service, "pushManager", null);

        assertThatThrownBy(() -> service.captcha(captchaInput(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息推送服务未启用");
    }

    @Test
    @DisplayName("captcha：租户未开放手机登录时抛出业务异常")
    void captchaThrowsWhenTenantDisallowsMobileLogin() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(featureService.getFeatureValue(TENANT_ID, UserFeatureProvider.APP_USER_ALLOW_MOBILE_LOGIN))
                .thenReturn("false");

        assertThatThrownBy(() -> service.captcha(captchaInput(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许手机登录");
    }

    @Test
    @DisplayName("captcha：手机号无对应账户时抛出业务异常")
    void captchaThrowsWhenAccountNotFound() {
        stubCountQuery(0L);

        assertThatThrownBy(() -> service.captcha(captchaInput(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统无此手机号账户");
    }

    @Test
    @DisplayName("captcha：发送频率过快时抛出业务异常")
    void captchaThrowsWhenTooFrequent() {
        stubCountQuery(1L);
        when(redisUtil.getCache(anyString())).thenReturn("1");

        assertThatThrownBy(() -> service.captcha(captchaInput(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发送频率过快");
    }

    @Test
    @DisplayName("captcha：校验通过时发送短信并写入缓存")
    void captchaSendsSmsAndCaches() {
        stubCountQuery(1L);
        when(redisUtil.getCache(anyString())).thenReturn(null);
        when(smsPushConfig.getSmsSignName()).thenReturn("sign");
        when(smsPushConfig.getSmsVerificationCode()).thenReturn("tpl");

        service.captcha(captchaInput(), request);

        verify(redisUtil, org.mockito.Mockito.times(2))
                .setCache(anyString(), anyString(), anyLong());
        verify(pushManager).smsPushAsync(any(PushSMS.class));
        verify(captchaService).setMobileSendCaptchaCount(request);
    }

    @Test
    @DisplayName("captcha：存在租户上下文时按租户维度查询账户")
    void captchaUsesTenantScopedQuery() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(featureService.getFeatureValue(TENANT_ID, UserFeatureProvider.APP_USER_ALLOW_MOBILE_LOGIN))
                .thenReturn("true");
        stubCountQuery(1L);
        when(redisUtil.getCache(anyString())).thenReturn(null);
        when(smsPushConfig.getSmsSignName()).thenReturn("sign");
        when(smsPushConfig.getSmsVerificationCode()).thenReturn("tpl");

        service.captcha(captchaInput(), request);

        verify(captchaService).verifyCaptchaSendMobile(any(), any());
    }

    // ---------------- login ----------------

    @Test
    @DisplayName("login：缓存验证码为空时抛出业务异常")
    void loginThrowsWhenCacheBlank() {
        when(redisUtil.getCache(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.login("13800000000", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码错误");
    }

    @Test
    @DisplayName("login：验证码不匹配时抛出业务异常")
    void loginThrowsWhenCaptchaMismatch() {
        when(redisUtil.getCache(anyString())).thenReturn("111111");

        assertThatThrownBy(() -> service.login("13800000000", "222222"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("login：验证码匹配时返回可登录用户列表")
    void loginSucceeds() {
        when(redisUtil.getCache(anyString())).thenReturn("111111");
        when(userRepository.findByPhoneNo("13800000000")).thenReturn(List.of(user(1L, TENANT_ID)));
        // 账户归属租户，必须显式放开手机登录特性，否则会被租户特性过滤器剔除
        when(featureService.getFeatureValue(TENANT_ID, UserFeatureProvider.APP_USER_ALLOW_MOBILE_LOGIN))
                .thenReturn("true");
        when(tokenAuthManager.generateToken(any())).thenReturn("token");

        List<MobileUserDto> result = service.login("13800000000", "111111");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getToken()).isEqualTo("token");
        verify(redisUtil).deleteCache(anyString());
    }

    // ---------------- getValidMobileUser ----------------

    @Test
    @DisplayName("getValidMobileUser：无租户上下文时按手机号全局查询")
    void getValidMobileUserWithoutTenant() {
        when(userRepository.findByPhoneNo("13800000000")).thenReturn(List.of(user(1L, null)));
        when(tokenAuthManager.generateToken(any())).thenReturn("token");

        assertThat(service.getValidMobileUser("13800000000")).hasSize(1);
    }

    @Test
    @DisplayName("getValidMobileUser：有租户上下文时按租户查询")
    void getValidMobileUserWithTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(userRepository.findByPhoneNoAndTenantId("13800000000", TENANT_ID))
                .thenReturn(List.of(user(1L, TENANT_ID)));
        when(featureService.getFeatureValue(TENANT_ID, UserFeatureProvider.APP_USER_ALLOW_MOBILE_LOGIN))
                .thenReturn("true");
        when(tokenAuthManager.generateToken(any())).thenReturn("token");

        assertThat(service.getValidMobileUser("13800000000")).hasSize(1);
    }

    @Test
    @DisplayName("getValidMobileUser：租户未开放手机登录时过滤该账户")
    void getValidMobileUserFiltersDisallowedTenant() {
        when(userRepository.findByPhoneNo("13800000000")).thenReturn(List.of(user(1L, TENANT_ID)));
        when(featureService.getFeatureValue(TENANT_ID, UserFeatureProvider.APP_USER_ALLOW_MOBILE_LOGIN))
                .thenReturn("false");

        assertThatThrownBy(() -> service.getValidMobileUser("13800000000"))
                .isInstanceOf(MobileAccountNotFoundException.class);
    }

    @Test
    @DisplayName("getValidMobileUser：唯一账户校验失败时抛出业务异常")
    void getValidMobileUserThrowsWhenSingleAccountInvalid() {
        when(userRepository.findByPhoneNo("13800000000")).thenReturn(List.of(user(1L, null)));
        doThrow(new BusinessException("账户已被锁定")).when(userService).checkUserValid(any());

        assertThatThrownBy(() -> service.getValidMobileUser("13800000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账户已被锁定");
    }

    @Test
    @DisplayName("getValidMobileUser：多账户中个别失败时跳过并返回其余可用账户")
    void getValidMobileUserSkipsInvalidAmongMultiple() {
        User first = user(1L, null);
        User second = user(2L, null);
        when(userRepository.findByPhoneNo("13800000000")).thenReturn(List.of(first, second));
        doThrow(new BusinessException("账户异常")).when(userService).checkUserValid(first);
        when(tokenAuthManager.generateToken(any())).thenReturn("token");

        List<MobileUserDto> result = service.getValidMobileUser("13800000000");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("sendCaptchaSms：组装短信并异步推送")
    void sendCaptchaSmsPushes() {
        when(smsPushConfig.getSmsSignName()).thenReturn("sign");
        when(smsPushConfig.getSmsVerificationCode()).thenReturn("tpl");

        service.sendCaptchaSms("13800000000", "123456");

        verify(pushManager).smsPushAsync(any(PushSMS.class));
    }
}
