package com.dusk.module.auth.registry.web;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;
import com.dusk.module.auth.registry.security.ServiceSecretVerifier;
import com.dusk.module.auth.registry.service.IResourceRegistryService;
import com.dusk.common.core.auth.registry.ResourceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link InternalResourceRegistryController} 的 HTTP 契约测试。
 *
 * <p>这里守的是<b>状态码语义</b>而不是业务逻辑：SDK 的 {@code SyncRegisterClient} 按状态码决定是否重试，
 * 4xx 会被当成确定性拒绝、5xx 会重试。把校验失败返回成 200（例如复用模块里把
 * {@code BusinessException} 映射为「HTTP 200 + 错误体」的基类）会让 SDK 认为注册成功——
 * 这正是本控制器不继承基类的原因，测试把它固定住。</p>
 */
class InternalResourceRegistryControllerTest {

    private static final String SECRET_HEADER = "X-Service-Secret";
    private static final String SECRET = "unit-test-secret";

    private final IResourceRegistryService registryService = mock(IResourceRegistryService.class);
    private final RegistryProperties properties = new RegistryProperties();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        properties.setServiceSecret(SECRET);
        properties.setServiceSecretHeader(SECRET_HEADER);

        InternalResourceRegistryController controller = new InternalResourceRegistryController();
        ReflectionTestUtils.setField(controller, "resourceRegistryService", registryService);
        ReflectionTestUtils.setField(controller, "serviceSecretVerifier", new ServiceSecretVerifier(properties));
        ReflectionTestUtils.setField(controller, "registryProperties", properties);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static String validBody() {
        return """
                {
                  "serviceId": "dusk-module-user",
                  "serviceVersion": "1.0.0",
                  "resourceVersion": "rv-1",
                  "resources": [
                    {"resourceId": "r1", "method": "GET", "path": "/users", "permission": "system:user:query"}
                  ],
                  "anonymousResources": [
                    {"method": "POST", "path": "/login"}
                  ]
                }
                """;
    }

    @Test
    @DisplayName("密钥错误或缺失：401（确定性拒绝，SDK 不重试）")
    void shouldRejectWhenSecretMissingOrWrong() throws Exception {
        mockMvc.perform(post("/internal/registry/snapshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(post("/internal/registry/snapshot")
                        .header(SECRET_HEADER, "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());

        verify(registryService, never()).register(any());
    }

    @Test
    @DisplayName("快照本身不合法：400（确定性拒绝），而不继承基类的「200 + 错误体」")
    void shouldReturnBadRequestForInvalidSnapshot() throws Exception {
        // serviceId 为空会被契约类型 ResourceSnapshot 的构造器拒绝，Jackson 包装后应为 400。
        mockMvc.perform(post("/internal/registry/snapshot")
                        .header(SECRET_HEADER, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceId": "", "serviceVersion": "1.0.0", "resourceVersion": "rv-1",
                                 "resources": [], "anonymousResources": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SNAPSHOT"));
    }

    @Test
    @DisplayName("领域校验失败（如 resourceVersion 与内容不一致）：400 且带原因")
    void shouldReturnBadRequestForRejectedSnapshot() throws Exception {
        when(registryService.register(any(ResourceSnapshot.class)))
                .thenThrow(new BusinessException("resourceVersion 与资源内容不一致"));

        mockMvc.perform(post("/internal/registry/snapshot")
                        .header(SECRET_HEADER, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SNAPSHOT_REJECTED"))
                .andExpect(jsonPath("$.message").value("resourceVersion 与资源内容不一致"));
    }

    @Test
    @DisplayName("服务端异常：500（可重试），交给 SDK 的重试语义")
    void shouldReturnServerErrorForUnexpectedFailure() throws Exception {
        when(registryService.register(any(ResourceSnapshot.class)))
                .thenThrow(new IllegalStateException("数据库不可用"));

        mockMvc.perform(post("/internal/registry/snapshot")
                        .header(SECRET_HEADER, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("REGISTRATION_FAILED"));
    }

    @Test
    @DisplayName("正常注册：200 并返回变更清单（SDK 据此判定 readiness 可放行）")
    void shouldAcceptValidSnapshot() throws Exception {
        when(registryService.register(any(ResourceSnapshot.class)))
                .thenReturn(new RegistryRegistrationResponse("dusk-module-user", "1.0.0", "rv-1", false,
                        "REGISTERED", 2, List.of("GET /users"), List.of(), List.of(), List.of(),
                        List.of("system:user:query"), List.of()));

        mockMvc.perform(post("/internal/registry/snapshot")
                        .header(SECRET_HEADER, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value("dusk-module-user"))
                .andExpect(jsonPath("$.resourceVersion").value("rv-1"))
                .andExpect(jsonPath("$.addedResources[0]").value("GET /users"));
    }
}
