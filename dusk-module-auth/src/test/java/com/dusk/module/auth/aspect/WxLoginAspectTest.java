package com.dusk.module.auth.aspect;

import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.weixin.WxMaSessionResult;
import com.dusk.module.auth.entity.UserWxRelation;
import com.dusk.module.auth.service.IUserWxRelationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * {@link WxLoginAspect} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WxLoginAspectTest {

    @Mock
    private IUserWxRelationService userWxRelationService;
    @Mock
    private ProceedingJoinPoint joinPoint;

    private WxLoginAspect aspect;

    private static MobileUserDto user(Long id, Long tenantId) {
        MobileUserDto user = new MobileUserDto();
        user.setId(id);
        user.setTenantId(tenantId);
        return user;
    }

    @BeforeEach
    void setUp() {
        aspect = new WxLoginAspect();
        ReflectionTestUtils.setField(aspect, "userWxRelationService", userWxRelationService);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"app-id", "open-id"});
    }

    @Test
    @DisplayName("aroundWxMicroProgramLogin：跳过无租户用户与已绑定用户，只保存新关系")
    void microProgramLogin_savesOnlyNewRelations() throws Throwable {
        List<MobileUserDto> users = new ArrayList<>(List.of(
                user(1L, null),
                user(2L, 10L),
                user(3L, 10L)));
        when(joinPoint.proceed()).thenReturn(users);
        when(userWxRelationService.getOpenId(2L, "app-id")).thenReturn("another-open-id");
        when(userWxRelationService.getOpenId(3L, "app-id")).thenReturn("OPEN-ID");

        Object result = aspect.aroundWxMicroProgramLogin(joinPoint);

        assertThat(result).isSameAs(users);
        ArgumentCaptor<List<UserWxRelation>> captor = ArgumentCaptor.forClass(List.class);
        verify(userWxRelationService).saveRelationList(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        UserWxRelation relation = captor.getValue().get(0);
        assertThat(relation.getUserId()).isEqualTo(2L);
        assertThat(relation.getAppId()).isEqualTo("app-id");
        assertThat(relation.getOpenId()).isEqualTo("open-id");
    }

    @Test
    @DisplayName("aroundWxMicroProgramLogin：所有用户都已绑定时不保存")
    void microProgramLogin_allBound() throws Throwable {
        List<MobileUserDto> users = new ArrayList<>(List.of(user(3L, 10L)));
        when(joinPoint.proceed()).thenReturn(users);
        when(userWxRelationService.getOpenId(3L, "app-id")).thenReturn("open-id");

        aspect.aroundWxMicroProgramLogin(joinPoint);

        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundWxMicroProgramLogin：空用户列表不保存")
    void microProgramLogin_emptyUsers() throws Throwable {
        when(joinPoint.proceed()).thenReturn(new ArrayList<>());

        aspect.aroundWxMicroProgramLogin(joinPoint);

        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundWxMicroProgramLogin：返回值不是用户列表时记录异常并原样返回")
    void microProgramLogin_wrongResultType() throws Throwable {
        when(joinPoint.proceed()).thenReturn("not-a-list");

        Object result = aspect.aroundWxMicroProgramLogin(joinPoint);

        assertThat(result).isEqualTo("not-a-list");
        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundWxMicroProgramLogin：保存关系异常时不影响登录结果")
    void microProgramLogin_saveFails() throws Throwable {
        List<MobileUserDto> users = new ArrayList<>(List.of(user(2L, 10L)));
        when(joinPoint.proceed()).thenReturn(users);
        when(userWxRelationService.getOpenId(2L, "app-id")).thenThrow(new IllegalStateException("db down"));

        Object result = aspect.aroundWxMicroProgramLogin(joinPoint);

        assertThat(result).isSameAs(users);
        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundGetSession：返回 null 时直接返回")
    void getSession_nullResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn(null);

        assertThat(aspect.aroundGetSession(joinPoint)).isNull();
        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundGetSession：openid 为 null 时直接返回")
    void getSession_nullOpenId() throws Throwable {
        WxMaSessionResult result = new WxMaSessionResult();
        result.setLoginData(List.of(user(2L, 10L)));
        when(joinPoint.proceed()).thenReturn(result);

        assertThat(aspect.aroundGetSession(joinPoint)).isSameAs(result);
        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundGetSession：loginData 为 null 时直接返回")
    void getSession_nullLoginData() throws Throwable {
        WxMaSessionResult result = new WxMaSessionResult();
        result.setOpenid("open-id");
        when(joinPoint.proceed()).thenReturn(result);

        assertThat(aspect.aroundGetSession(joinPoint)).isSameAs(result);
        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundGetSession：loginData 为空时直接返回")
    void getSession_emptyLoginData() throws Throwable {
        WxMaSessionResult result = new WxMaSessionResult();
        result.setOpenid("open-id");
        result.setLoginData(List.of());
        when(joinPoint.proceed()).thenReturn(result);

        assertThat(aspect.aroundGetSession(joinPoint)).isSameAs(result);
        verify(userWxRelationService, never()).saveRelationList(anyList());
    }

    @Test
    @DisplayName("aroundGetSession：登录数据有效时保存微信关系")
    void getSession_savesRelations() throws Throwable {
        WxMaSessionResult result = new WxMaSessionResult();
        result.setOpenid("OPEN-ID");
        result.setLoginData(List.of(user(2L, 10L)));
        when(joinPoint.proceed()).thenReturn(result);
        when(userWxRelationService.getOpenId(2L, "app-id")).thenReturn("another");

        assertThat(aspect.aroundGetSession(joinPoint)).isSameAs(result);
        ArgumentCaptor<List<UserWxRelation>> captor = ArgumentCaptor.forClass(List.class);
        verify(userWxRelationService).saveRelationList(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getOpenId()).isEqualTo("OPEN-ID");
    }

    @Test
    @DisplayName("aroundGetSession：保存异常时不影响会话结果")
    void getSession_saveFails() throws Throwable {
        WxMaSessionResult result = new WxMaSessionResult();
        result.setOpenid("OPEN-ID");
        result.setLoginData(List.of(user(2L, 10L)));
        when(joinPoint.proceed()).thenReturn(result);
        when(userWxRelationService.getOpenId(2L, "app-id")).thenThrow(new IllegalStateException("db down"));

        assertThat(aspect.aroundGetSession(joinPoint)).isSameAs(result);
        verify(userWxRelationService, never()).saveRelationList(anyList());
    }
}
