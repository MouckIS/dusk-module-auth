package com.dusk.module.auth.dto.internal;

/**
 * 内网「关联厂站」请求体，对应原 {@code IAuthRpcService#getLinkedOrgIds(String, String)}。
 *
 * @param orgId          请求带来的组织/厂站 id，可为空（表示由 Auth 依据登录用户推导默认厂站）
 * @param authentication 完整的 Authorization 头值（含 {@code Bearer } 前缀）
 * @author kefuming
 */
public record LinkedOrgIdsRequest(String orgId, String authentication) {
}
