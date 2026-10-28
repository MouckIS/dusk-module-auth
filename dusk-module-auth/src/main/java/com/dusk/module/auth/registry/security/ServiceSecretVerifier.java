package com.dusk.module.auth.registry.security;

import com.dusk.module.auth.registry.config.RegistryProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 注册端点的服务间密钥校验，见《权限优化方案-整理版》3.5。
 *
 * <p><b>为什么不是登录态</b>：同步注册发生在服务启动期、readiness 放行之前，
 * 此时不存在任何登录用户与租户上下文；调用方是「服务」而不是「人」，
 * 因此 3.5 明确该端点的身份来自「HTTP 内网端点 + 服务间密钥」，
 * 而不是 JWT、也不是 3.10 明令禁止的 {@code SDK → dusk-auth-api → Dubbo} 反向依赖。</p>
 *
 * <p><b>失效方向</b>：默认 fail-closed（{@code app.registry.require-service-secret=true}），
 * 即未配置密钥时拒绝一切请求，避免「漏配密钥 = 端点对外开放」。只有显式配置为 {@code false}
 * 才允许空密钥放行，用于内网开发环境。</p>
 */
@Component
@Slf4j
public class ServiceSecretVerifier {

    private final RegistryProperties properties;

    public ServiceSecretVerifier(RegistryProperties properties) {
        this.properties = properties;
    }

    /**
     * 启动期把配置状态显式打出来。
     *
     * <p>注册端点写入的是权限事实，其身份强度必须可审计——不能只靠「调用方记得配密钥」。</p>
     */
    @PostConstruct
    void logConfigurationState() {
        if (StringUtils.hasText(properties.getServiceSecret())) {
            log.info("权限注册端点已启用服务间密钥校验，请求头：{}", properties.getServiceSecretHeader());
            return;
        }
        if (properties.isRequireServiceSecret()) {
            log.warn("权限注册端点未配置服务间密钥（{} 为空），当前为 fail-closed：将拒绝所有注册请求。"
                            + "请在 Auth 与各业务服务两侧同时配置同一密钥（Auth 侧配置项 app.registry.service-secret）。",
                    "app.registry.service-secret");
        } else {
            log.warn("权限注册端点未配置服务间密钥且已显式关闭强制校验（app.registry.require-service-secret=false），"
                    + "当前任何可访问该路径的调用方都能改写权限注册表。仅限内网开发环境使用。");
        }
    }

    /**
     * 判断本次请求是否通过校验。
     *
     * @param presentedSecret 请求头中携带的密钥，可为 {@code null}
     */
    public boolean isAuthorized(String presentedSecret) {
        return matches(properties.getServiceSecret(), presentedSecret, properties.isRequireServiceSecret());
    }

    /**
     * 恒定时间比较，避免通过响应时间推断密钥内容。
     *
     * @param expected          配置的密钥，空白表示未配置
     * @param presented         请求携带的密钥
     * @param requiredWhenBlank 未配置密钥时是否仍要求通过校验（{@code false} 表示放行）
     */
    static boolean matches(String expected, String presented, boolean requiredWhenBlank) {
        if (!StringUtils.hasText(expected)) {
            return !requiredWhenBlank;
        }
        if (!StringUtils.hasText(presented)) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.trim().getBytes(StandardCharsets.UTF_8),
                presented.trim().getBytes(StandardCharsets.UTF_8));
    }
}
