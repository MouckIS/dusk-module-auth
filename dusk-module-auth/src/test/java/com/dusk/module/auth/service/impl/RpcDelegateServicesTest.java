package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.AuditLogDto;
import com.dusk.module.auth.dto.GenerateTokenForNonUserInput;
import com.dusk.module.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.module.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.module.auth.service.ITokenAuthRpcService;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.dto.token.TokenSign;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.repository.IAuditLogRepository;
import com.dusk.module.auth.service.IUserFingerprintService;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 门禁包内 4 个「薄委托型」实现类的单元测试：
 * <ul>
 *   <li>{@link TokenServiceImpl} —— 永久 Token 签发委托</li>
 *   <li>{@link SmRpcUtil} —— SM4 加解密工具 RPC 实现</li>
 *   <li>{@link AuditLogRpcServiceImpl} —— 审计日志落库委托</li>
 *   <li>{@link UserFingerprintRpcServiceImpl} —— 用户指纹查询委托</li>
 * </ul>
 * 合计约 17 行、0 分支，合并为一个测试类以避免为每个类建立近乎空的测试文件。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RpcDelegateServicesTest {

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

    @Mock
    private ITokenAuthRpcService tokenAuthRpcService;
    @Mock
    private AppAuthConfig appAuthConfig;
    @Mock
    private IAuditLogRepository auditLogRepository;
    @Mock
    private IUserFingerprintService userFingerprintService;

    private TokenServiceImpl tokenService;
    private SmRpcUtil smRpcUtil;
    private AuditLogRpcServiceImpl auditLogRpcService;
    private UserFingerprintRpcServiceImpl userFingerprintRpcService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "tokenAuthRpcService", tokenAuthRpcService);

        smRpcUtil = new SmRpcUtil();
        ReflectionTestUtils.setField(smRpcUtil, "appAuthConfig", appAuthConfig);
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(SM4_KEY);

        auditLogRpcService = new AuditLogRpcServiceImpl();
        ReflectionTestUtils.setField(auditLogRpcService, "repository", auditLogRepository);

        userFingerprintRpcService = new UserFingerprintRpcServiceImpl();
        ReflectionTestUtils.setField(userFingerprintRpcService, "userFingerprintService", userFingerprintService);
    }

    // ------------------------------------------------------------------
    // TokenServiceImpl
    // ------------------------------------------------------------------

    @Test
    @DisplayName("foreverTokenSign：复制标识与角色，时间单位固定为天并委托 RPC")
    void foreverTokenSign() {
        TokenSign sign = new TokenSign();
        sign.setIdentify("user-1");
        sign.setRoles(List.of("admin", "user"));
        sign.setTime(365L);
        when(tokenAuthRpcService.generateTokenForNonUser(any(GenerateTokenForNonUserInput.class)))
                .thenReturn("signed-token");

        String result = tokenService.foreverTokenSign(sign);

        assertThat(result).isEqualTo("signed-token");
        ArgumentCaptor<GenerateTokenForNonUserInput> captor =
                ArgumentCaptor.forClass(GenerateTokenForNonUserInput.class);
        verify(tokenAuthRpcService).generateTokenForNonUser(captor.capture());
        assertThat(captor.getValue().getIdentify()).isEqualTo("user-1");
        assertThat(captor.getValue().getRoles()).containsExactly("admin", "user");
        assertThat(captor.getValue().getTime()).isEqualTo(365L);
        assertThat(captor.getValue().getUnit()).isEqualTo(TimeUnit.DAYS);
    }

    // ------------------------------------------------------------------
    // SmRpcUtil
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sm4EncryptHex / sm4DecryptStr：十六进制加解密可还原")
    void sm4HexRoundTrip() {
        String cipher = smRpcUtil.sm4EncryptHex("dusk-ms");

        assertThat(cipher).isNotBlank().isNotEqualTo("dusk-ms");
        assertThat(smRpcUtil.sm4DecryptStr(cipher)).isEqualTo("dusk-ms");
    }

    @Test
    @DisplayName("sm4EncryptBase64：Base64 密文可还原")
    void sm4Base64RoundTrip() {
        String cipher = smRpcUtil.sm4EncryptBase64("dusk-ms");

        assertThat(smRpcUtil.sm4DecryptStr(cipher)).isEqualTo("dusk-ms");
    }

    @Test
    @DisplayName("sm4EncryptHex：相同明文加密结果稳定（ECB 模式）")
    void sm4EncryptDeterministic() {
        assertThat(smRpcUtil.sm4EncryptHex("same"))
                .isEqualTo(smRpcUtil.sm4EncryptHex("same"));
    }

    // ------------------------------------------------------------------
    // AuditLogRpcServiceImpl
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saveLog：拷贝属性并落库")
    void saveLog() {
        AuditLogDto dto = new AuditLogDto();
        dto.setMethodName("com.dusk.Foo.bar");
        dto.setClientIpAddress("127.0.0.1");
        dto.setExecutionDuration(12);
        dto.setTenantId(3L);

        auditLogRpcService.saveLog(dto);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMethodName()).isEqualTo("com.dusk.Foo.bar");
        assertThat(captor.getValue().getExecutionDuration()).isEqualTo(12);
        assertThat(captor.getValue().getTenantId()).isEqualTo(3L);
    }

    // ------------------------------------------------------------------
    // UserFingerprintRpcServiceImpl
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getAll：委托业务服务查询")
    void userFingerprintGetAll() {
        GetAllInputDto input = new GetAllInputDto();
        input.setUserIds(List.of(1L, 2L));
        when(userFingerprintService.getAll(input)).thenReturn(List.of(new UserFingerprintDto()));

        assertThat(userFingerprintRpcService.getAll(input)).hasSize(1);
        verify(userFingerprintService).getAll(input);
    }
}
