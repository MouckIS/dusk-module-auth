package com.dusk.module.auth.registry.web;

import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.IgnoreResponseAdvice;
import com.dusk.common.rpc.authz.IAuthorizationRuntimeRpcService;
import com.dusk.common.rpc.authz.ResourceRuntimeSnapshot;
import com.dusk.common.rpc.authz.RolePermissionSnapshot;
import com.dusk.common.rpc.authz.SubjectSnapshot;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.security.ServiceSecretVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 新权限模型的运行态回源内网端点，替代网关到 Auth 的 Dubbo 回源。
 *
 * <p>网关的 {@code DubboRuntimeAuthorizationFallback} 原先通过
 * {@link IAuthorizationRuntimeRpcService} 在 L1/L2 都未命中时回源，去 Dubbo 后改为调用本控制器。
 * 语义逐条对齐原接口契约（见 {@code dusk-common-rpc} 的 {@code authz} 包 javadoc）：</p>
 * <ul>
 *   <li>只在缓存未命中时调用，结果带版本号；</li>
 *   <li>查不到数据返回 {@code 204 No Content}（原接口返回 {@code null}），
 *       让网关的 {@code fallback} 分支与之前完全一致；</li>
 *   <li>服务端异常返回 5xx，网关按 {@code FallbackStatus.UNAVAILABLE} 处理（fail-closed）。</li>
 * </ul>
 *
 * <p>作用域参数用查询串传 {@code tenantScope}（形如 {@code HOST} 或 {@code TENANT:12}）：
 * 冒号在路径段里需要转义，放在查询串里既避免转义问题也让日志更可读。</p>
 *
 * @author kefuming
 */
@RestController
@RequestMapping("/internal/auth/runtime")
@IgnoreResponseAdvice
@Tag(name = "InternalAuthorizationRuntime", description = "运行态回源（内网，服务间密钥鉴权）")
@Slf4j
public class InternalAuthorizationRuntimeController {

    @Resource
    private IAuthorizationRuntimeRpcService authorizationRuntimeRpcService;
    @Resource
    private ServiceSecretVerifier serviceSecretVerifier;
    @Resource
    private RegistryProperties registryProperties;

    /**
     * 某服务当前有效的资源并集。
     *
     * @param serviceId 服务名（网关取自路由 URI 的 host）
     */
    @Operation(summary = "查询服务资源并集（内网端点）")
    @GetMapping("/resource-snapshot")
    @AllowAnonymous
    public ResponseEntity<Object> resourceSnapshot(@RequestParam(required = false) String serviceId,
                                                   @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        ResourceRuntimeSnapshot snapshot = authorizationRuntimeRpcService.getResourceSnapshot(serviceId);
        return snapshot == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(snapshot);
    }

    /**
     * 某作用域下某角色的有效权限。
     *
     * @param tenantScope 作用域，{@code HOST} 或 {@code TENANT:<id>}
     * @param roleId      角色 id
     */
    @Operation(summary = "查询角色权限（内网端点）")
    @GetMapping("/role-permissions")
    @AllowAnonymous
    public ResponseEntity<Object> rolePermissions(@RequestParam(required = false) String tenantScope,
                                                  @RequestParam(required = false) Long roleId,
                                                  @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        RolePermissionSnapshot snapshot = authorizationRuntimeRpcService.getRolePermissions(tenantScope, roleId);
        return snapshot == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(snapshot);
    }

    /**
     * 某作用域下某账号的状态与代际。
     *
     * @param tenantScope 作用域，{@code HOST} 或 {@code TENANT:<id>}
     * @param userId      用户 id
     */
    @Operation(summary = "查询账号状态（内网端点）")
    @GetMapping("/subject")
    @AllowAnonymous
    public ResponseEntity<Object> subject(@RequestParam(required = false) String tenantScope,
                                          @RequestParam(required = false) Long userId,
                                          @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        SubjectSnapshot snapshot = authorizationRuntimeRpcService.getSubject(tenantScope, userId);
        return snapshot == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(snapshot);
    }

    /**
     * 服务间密钥校验。
     *
     * @return 校验失败时的 401 响应；通过时返回 {@code null}
     */
    private ResponseEntity<Object> verifySecret(HttpHeaders headers) {
        String presentedSecret = headers.getFirst(registryProperties.getServiceSecretHeader());
        if (serviceSecretVerifier.isAuthorized(presentedSecret)) {
            return null;
        }
        log.error("运行态回源内网端点被拒绝：服务间密钥校验未通过（请求头 {} 缺失或不匹配）",
                registryProperties.getServiceSecretHeader());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorBody(
                "UNAUTHORIZED",
                "服务间密钥校验未通过，请确认请求头 " + registryProperties.getServiceSecretHeader()
                        + " 与 Auth 侧 app.registry.service-secret 配置一致"));
    }

    /**
     * 统一 5xx：网关按 {@code UNAVAILABLE} 处理，保持 fail-closed。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception e) {
        log.error("运行态回源内网端点服务端异常", e);
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName() : e.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("RUNTIME_RESOLVE_FAILED", message));
    }

    /**
     * 错误正文，保持极小且稳定。
     *
     * @param error   稳定的机器可读标识
     * @param message 人类可读说明
     */
    public record ErrorBody(String error, String message) {
    }
}
