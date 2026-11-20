package com.dusk.module.auth.dto.internal;

/**
 * 内网鉴权请求体，供网关通过 HTTP 调用 Auth 的 {@code /internal/auth/check} 端点。
 *
 * <p>单体化后网关与单体是两个进程，原 {@code IAuthRpcService#auth} 的 Dubbo 通道已移除，
 * 因此把同样的三个入参显式声明成可序列化契约，避免网关侧手拼 JSON。</p>
 *
 * @param authorization   请求头里的完整 Authorization 值（含 {@code Bearer } 前缀），可为空
 * @param applicationName 目标服务名（网关取自路由 URI 的 host）
 * @param url             服务内路径（StripPrefix 之后的路径，红线⑫）
 * @author kefuming
 */
public record AuthCheckRequest(String authorization, String applicationName, String url) {
}
