package com.dusk.module.auth.registry.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 权限注册中心配置，前缀 {@code app.registry}。
 *
 * <p>对应《权限优化方案-整理版》3.5（服务间密钥）与 3.8 / 5.7（Redis 运行读模型）。</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.registry")
@Getter
@Setter
public class RegistryProperties {

    /**
     * 服务间密钥。SDK 通过 {@link #serviceSecretHeader} 指定的请求头携带，两侧必须配置为同一值。
     *
     * <p>为空是否放行由 {@link #requireServiceSecret} 决定。<b>不建议留空</b>：
     * 注册端点直接写入权限事实（PG），一旦网关可达该路径，等于任何人都能改写权限注册表。</p>
     */
    private String serviceSecret;

    /**
     * 承载服务间密钥的请求头名，需与 SDK 的 {@code app.permission.sdk.service-secret-header} 保持一致。
     */
    private String serviceSecretHeader = "X-Service-Secret";

    /**
     * 是否强制要求服务间密钥。
     *
     * <p>默认为 {@code true}：<b>未配置密钥时拒绝一切注册请求</b>（fail-closed，安全默认）。
     * 仅内网开发环境可显式设为 {@code false} 放行，且此时仍要求「配置了密钥就必须匹配」。
     * 这样本机脱网联调不必配置密钥，而生产不会因为漏配而静默开放。</p>
     */
    private boolean requireServiceSecret = true;

    /**
     * 是否强制校验快照自带的 {@code resourceVersion} 与内容一致（3.3 契约）。
     *
     * <p>默认开启：该版本号是 Diff 幂等的唯一依据，若 SDK 上报的版本号与其资源内容不符，
     * 会出现「内容变了但版本没变 → 整个版本被跳过」的静默失配，比拒绝注册危险得多。</p>
     */
    private boolean strictResourceVersion = true;

    /**
     * Redis 运行读模型（3.4 第 12 步 / 5.7）。
     */
    private ReadModel readModel = new ReadModel();

    @Getter
    @Setter
    public static class ReadModel {

        /**
         * 是否在注册事务提交后把 {@code auth:resource:{serviceId}} 投影到 Redis。
         *
         * <p>关闭后本模块只写 PostgreSQL（事实来源），网关侧缓存将恒未命中并回源。</p>
         */
        private boolean enabled = true;

        /**
         * 读模型条目的存活时间（5.4 的 L2 TTL）。
         *
         * <p>TTL 的作用是给「投影写入失败 / 事件丢失」一个上界，避免一份过期的资源映射长期驻留；
         * 到期后网关走 L2 未命中 → 回源分支，再在下一次注册时重建。</p>
         */
        private Duration ttl = Duration.ofMinutes(30);
    }
}
