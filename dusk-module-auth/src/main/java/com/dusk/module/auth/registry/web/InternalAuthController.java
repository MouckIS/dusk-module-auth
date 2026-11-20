package com.dusk.module.auth.registry.web;

import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.IgnoreResponseAdvice;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.UserContextException;
import com.dusk.module.auth.dto.TenantInfoDto;
import com.dusk.module.auth.dto.internal.AuthCheckRequest;
import com.dusk.module.auth.dto.internal.AuthCheckResponse;
import com.dusk.module.auth.dto.internal.LinkedOrgIdsRequest;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.security.ServiceSecretVerifier;
import com.dusk.module.auth.service.IAuthRpcService;
import com.dusk.module.auth.service.ISsoTokenService;
import com.dusk.module.auth.service.ITenantRpcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关鉴权内网端点，替代单体化之前网关到 Auth 的 Dubbo 调用。
 *
 * <p>单体化把 5 个业务服务合并为 {@code dusk-module-app}，网关仍作为独立进程存在
 * （WebFlux 与 WebMVC 不能共享 ApplicationContext）。去掉 Dubbo 后，网关原先的 6 个
 * {@code @DubboReference} 调用点必须有等价的进程间通道，本控制器提供其中 5 个：</p>
 *
 * <table border="1">
 *   <caption>Dubbo → HTTP 映射</caption>
 *   <tr><th>原 Dubbo 方法</th><th>本端点</th><th>调用方</th></tr>
 *   <tr><td>{@code IAuthRpcService#auth}</td><td>{@code POST /internal/auth/check}</td><td>{@code AccessGatewayFilter}</td></tr>
 *   <tr><td>{@code IAuthRpcService#getLinkedOrgIds}</td><td>{@code POST /internal/auth/linked-org-ids}</td><td>{@code DataPermissionFilter}</td></tr>
 *   <tr><td>{@code IAuthRpcService#changeRealToken}</td><td>{@code POST /internal/auth/change-real-token}</td><td>{@code TokenTransferFilter}</td></tr>
 *   <tr><td>{@code ITenantRpcService#findByTenantName}</td><td>{@code GET /internal/auth/tenant}</td><td>{@code AccessGatewayFilter} / {@code AppVersionGatewayFilter}</td></tr>
 *   <tr><td>{@code ISsoTokenRpcService#ssoSm4Token}</td><td>{@code POST /internal/auth/sso-sm4-token}</td><td>{@code AccessGatewayFilter}</td></tr>
 * </table>
 *
 * <h3>身份来自服务间密钥，不是登录态</h3>
 * <p>这些端点由网关在<b>鉴权之前</b>调用，此时请求可能还没有合法登录态，
 * 因此不能要求 JWT。身份由 {@code app.registry.service-secret} 对应的请求头承载，
 * 复用注册端点同一套 {@link ServiceSecretVerifier}（默认 fail-closed）。</p>
 *
 * <h3>为什么不继承 CruxBaseController</h3>
 * <p>与 {@link InternalResourceRegistryController} 同因：基类把 {@link BusinessException}
 * 映射为「HTTP 200 + 错误正文」，而网关侧需要靠真实状态码区分
 * 「未登录（401）/ 无权限（403）/ 服务不可用（5xx）」。因此本控制器自行处理异常，
 * 并用 {@link IgnoreResponseAdvice} 避免全局响应包装。</p>
 *
 * @author kefuming
 */
@RestController
@RequestMapping("/internal/auth")
@IgnoreResponseAdvice
@Tag(name = "InternalAuth", description = "网关鉴权（内网，服务间密钥鉴权）")
@Slf4j
public class InternalAuthController {

    @Resource
    private IAuthRpcService authRpcService;
    @Resource
    private ITenantRpcService tenantRpcService;
    @Resource
    private ISsoTokenService ssoTokenService;
    @Resource
    private ServiceSecretVerifier serviceSecretVerifier;
    @Resource
    private RegistryProperties registryProperties;

    /**
     * 校验某次请求是否有权访问目标服务与路径。
     *
     * <p>与原 Dubbo 语义逐条对齐：白名单命中直接放行；否则校验令牌有效性，
     * 令牌无效抛 {@link UserContextException}（映射为 401），有令牌但无权限返回
     * {@code allowed=false}（网关回 403）。</p>
     */
    @Operation(summary = "校验请求权限（内网端点）")
    @PostMapping("/check")
    @AllowAnonymous
    public ResponseEntity<Object> check(@RequestBody(required = false) AuthCheckRequest request,
                                        @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        if (request == null || request.applicationName() == null || request.applicationName().isBlank()
                || request.url() == null || request.url().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST",
                    "applicationName 与 url 不能为空"));
        }
        try {
            boolean allowed = authRpcService.auth(request.authorization(), request.applicationName(), request.url());
            return ResponseEntity.ok(allowed ? AuthCheckResponse.allow() : AuthCheckResponse.deny());
        } catch (UserContextException e) {
            // 401 是确定性结论：网关据此返回「尚未登陆」，不应重试。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthCheckResponse.unauthenticated(e.getMessage()));
        }
    }

    /**
     * 取登录用户关联的组织/厂站 id，供网关回填数据权限请求头。
     */
    @Operation(summary = "获取关联厂站 id（内网端点）")
    @PostMapping("/linked-org-ids")
    @AllowAnonymous
    public ResponseEntity<Object> linkedOrgIds(@RequestBody(required = false) LinkedOrgIdsRequest request,
                                               @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST", "请求体不能为空"));
        }
        return ResponseEntity.ok(authRpcService.getLinkedOrgIds(request.orgId(), request.authentication()));
    }

    /**
     * 把 token-id 换成真实 JWT，兼容「一次一密」的令牌下发方式。
     *
     * @param tokenId 令牌 id，来自 {@code token-id} 请求头或 {@code tokenId} 查询参数
     */
    @Operation(summary = "tokenId 换真实 token（内网端点）")
    @PostMapping("/change-real-token")
    @AllowAnonymous
    public ResponseEntity<Object> changeRealToken(@RequestParam(required = false) String tokenId,
                                                  @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        String jwt = authRpcService.changeRealToken(tokenId);
        return ResponseEntity.ok(jwt == null ? "" : jwt);
    }

    /**
     * 按租户名查租户信息，供网关做 SSO 与 App 版本校验。
     *
     * @return 租户不存在时返回 {@code 204 No Content}（与原 Dubbo 返回 {@code null} 语义一致）
     */
    @Operation(summary = "按租户名查租户（内网端点）")
    @GetMapping("/tenant")
    @AllowAnonymous
    public ResponseEntity<Object> tenant(@RequestParam(required = false) String tenantName,
                                         @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        TenantInfoDto tenantInfo = tenantRpcService.findByTenantName(tenantName);
        if (tenantInfo == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(tenantInfo);
    }

    /**
     * 国密 SM4 密文换 JWT，对应原 {@code ISsoTokenRpcService#ssoSm4Token}。
     *
     * <p>业务异常（密文非法、票据过期、重放）以 400 返回，网关会把它转成
     * 「HTTP 200 + 业务错误体」的既有前端契约。</p>
     */
    @Operation(summary = "国密 SSO 换 token（内网端点）")
    @PostMapping("/sso-sm4-token")
    @AllowAnonymous
    public ResponseEntity<Object> ssoSm4Token(@RequestParam(required = false) String encryptStr,
                                              @RequestHeader HttpHeaders headers) {
        ResponseEntity<Object> rejected = verifySecret(headers);
        if (rejected != null) {
            return rejected;
        }
        return ResponseEntity.ok(ssoTokenService.ssoSm4Token(encryptStr));
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
        log.error("网关鉴权内网端点被拒绝：服务间密钥校验未通过（请求头 {} 缺失或不匹配）",
                registryProperties.getServiceSecretHeader());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorBody(
                "UNAUTHORIZED",
                "服务间密钥校验未通过，请确认请求头 " + registryProperties.getServiceSecretHeader()
                        + " 与 Auth 侧 app.registry.service-secret 配置一致"));
    }

    /**
     * 业务异常统一转 4xx：参数错误、密文非法都属确定性失败，网关不应重试。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException e) {
        log.error("网关鉴权内网端点业务异常：{}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorBody("BUSINESS_REJECTED", e.getMessage()));
    }

    /**
     * 其余异常一律 5xx：网关据此判定「Auth 不可用」，按既有 fail-open/fail-closed 策略处理。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception e) {
        log.error("网关鉴权内网端点服务端异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("AUTH_CALL_FAILED", "服务端异常：" + rootMessage(e)));
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
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
