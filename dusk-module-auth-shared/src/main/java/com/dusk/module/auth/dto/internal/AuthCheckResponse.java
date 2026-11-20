package com.dusk.module.auth.dto.internal;

/**
 * 内网鉴权响应体，见 {@link AuthCheckRequest}。
 *
 * <p>刻意用不可变 record 且只暴露三种语义，让网关侧不必解析错误正文即可判定处理分支：</p>
 * <ul>
 *   <li>{@code allowed=true}：放行；</li>
 *   <li>{@code allowed=false} 且 {@code unauthenticated=false}：已登录但无权限 → 网关回 403；</li>
 *   <li>{@code allowed=false} 且 {@code unauthenticated=true}：未登录 / 令牌无效 → 网关回 401。</li>
 * </ul>
 *
 * @param allowed         是否放行
 * @param unauthenticated 是否属于「尚未登陆」语义
 * @param message         人类可读说明，仅用于日志
 * @author kefuming
 */
public record AuthCheckResponse(boolean allowed, boolean unauthenticated, String message) {

    public static AuthCheckResponse allow() {
        return new AuthCheckResponse(true, false, "放行");
    }

    public static AuthCheckResponse deny() {
        return new AuthCheckResponse(false, false, "无权限访问");
    }

    public static AuthCheckResponse unauthenticated(String message) {
        return new AuthCheckResponse(false, true, message);
    }
}
