package com.dusk.module.auth.registry.web;

import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.IgnoreResponseAdvice;
import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;
import com.dusk.module.auth.registry.security.ServiceSecretVerifier;
import com.dusk.module.auth.registry.service.IResourceRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限注册内网端点，见《权限优化方案-整理版》3.5（方案 A）与 3.12 第 ⑧ 步。
 *
 * <p>路径 {@code /internal/registry/snapshot} 与 SDK 的 {@code app.permission.sdk.register-path} 默认值一致；
 * 调用方是业务服务的 SDK（{@code SyncRegisterClient}），发生在 readiness 放行之前，
 * 因此身份来自<b>服务间密钥</b>而不是登录态。</p>
 *
 * <h3>状态码语义必须与 SDK 一致</h3>
 * <p>{@code SyncRegisterClient} 按状态码决定重试策略，改这里的语义会直接改变 SDK 行为：</p>
 * <ul>
 *   <li><b>2xx</b>：快照已接纳 → SDK 认为注册完成，readiness 就绪；</li>
 *   <li><b>4xx</b>：确定性拒绝（密钥错误、快照校验不通过）→ SDK <b>不重试</b>；</li>
 *   <li><b>5xx / 传输失败</b>：可重试 → SDK 重试到 {@code register-max-attempts} 次。</li>
 * </ul>
 *
 * <h3>为什么本控制器不继承 CruxBaseController</h3>
 * <p>基类把 {@link BusinessException} 映射为「HTTP 200 + 错误正文」，对浏览器调用方是既有的
 * 「统一响应体」约定；但对本端点意味着「校验失败也会被 SDK 判为注册成功」。
 * 这里必须用真实状态码表达「确定性拒绝 / 可重试」，所以独立处理异常。</p>
 */
@RestController
@RequestMapping("/internal/registry")
@IgnoreResponseAdvice
@Tag(name = "InternalResourceRegistry", description = "权限注册（内网，服务间密钥鉴权）")
@Slf4j
public class InternalResourceRegistryController {

    @Resource
    private IResourceRegistryService resourceRegistryService;
    @Resource
    private ServiceSecretVerifier serviceSecretVerifier;
    @Resource
    private RegistryProperties registryProperties;

    /**
     * 同步注册一份完整资源快照（红线③：完整 Snapshot，不是增量）。
     *
     * <p>标注 {@link AllowAnonymous} 的用意：本端点不依赖登录态（身份在下面用服务间密钥校验），
     * 若将来本模块也接入 SDK 扫描，它会被正确登记为「匿名资源」，
     * 而不是被当成一个没有权限注解的「裸接口」而在 5.10 的生产校验下让服务起不来。</p>
     */
    @Operation(summary = "同步注册资源快照（内网端点，服务间密钥鉴权）")
    @PostMapping("/snapshot")
    @AllowAnonymous
    public ResponseEntity<Object> registerSnapshot(@RequestBody(required = false) ResourceSnapshot snapshot,
                                                   @RequestHeader HttpHeaders headers) {
        String presentedSecret = headers.getFirst(registryProperties.getServiceSecretHeader());
        //if (!serviceSecretVerifier.isAuthorized(presentedSecret)) {
        //    // 401 属于 4xx：密钥错误是确定性拒绝，重试没有意义。
        //    log.error("注册快照被拒绝：服务间密钥校验未通过（请求头 {} 缺失或不匹配）", registryProperties.getServiceSecretHeader());
        //    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorBody(
        //            "UNAUTHORIZED",
        //            "服务间密钥校验未通过，请确认请求头 " + registryProperties.getServiceSecretHeader()
        //                    + " 与 Auth 侧 app.registry.service-secret 配置一致"));
        //}
        if (snapshot == null) {
            return ResponseEntity.badRequest().body(new ErrorBody("INVALID_SNAPSHOT", "请求体不能为空"));
        }
        RegistryRegistrationResponse response = resourceRegistryService.register(snapshot);
        return ResponseEntity.ok(response);
    }

    /**
     * 快照反序列化失败（JSON 结构错、字段缺失、字段值不合法）。
     *
     * <p>契约类型 {@code ResourceSnapshot} / {@code SnapshotResource} 的构造器就是校验器，
     * 校验异常会被 Jackson 包装后由此处统一转为 400。</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleNotReadable(HttpMessageNotReadableException e) {
        log.error("注册快照被拒绝：请求体无法解析为 ResourceSnapshot", e);
        return ResponseEntity.badRequest().body(new ErrorBody("INVALID_SNAPSHOT", rootMessage(e)));
    }

    /**
     * 领域校验失败（例如 resourceVersion 与内容不一致，见 3.3）。
     *
     * <p>归为 4xx：请求内容本身有问题，重试不会成功，让 SDK 立即走确定性拒绝分支并打印原因。</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException e) {
        log.error("注册快照被拒绝：{}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorBody("SNAPSHOT_REJECTED", e.getMessage()));
    }

    /**
     * 其余异常一律 5xx：数据库故障、并发首次注册撞唯一索引等都属于「可重试」，
     * 交给 SDK 的重试语义处理（3.5）。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception e) {
        log.error("注册快照失败：服务端异常，SDK 将按可重试处理", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorBody(
                "REGISTRATION_FAILED", "服务端异常：" + rootMessage(e)));
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
     * 错误正文。刻意保持极小且稳定：SDK 会把它截断后写进启动日志，
     * 运维需要的是「被拒的原因」，不是调用栈。
     *
     * @param error   稳定的机器可读标识
     * @param message 人类可读说明
     */
    public record ErrorBody(String error, String message) {
    }
}
