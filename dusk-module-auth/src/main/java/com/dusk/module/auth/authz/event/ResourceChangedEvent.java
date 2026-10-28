package com.dusk.module.auth.authz.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 资源定义变更事件载荷（Auth → Gateway），见《权限优化方案-整理版》7.2。
 *
 * <p><b>跨仓契约</b>：字段名必须与 Gateway 侧
 * {@code com.dusk.gateway.authz.event.ResourceChangedEvent} 逐字一致。</p>
 *
 * <p>与 {@link PermissionChangedEvent} 的关键差别：{@code resourceVersion} 是
 * {@code ResourceVersionCalculator} 算出的 <b>SHA-256 内容哈希</b>，只有「相等/不等」语义；
 * 新旧判断一律使用 Auth 分配的单调 {@code resourceRevision}（红线⑨）。</p>
 *
 * @param serviceId        服务标识
 * @param resourceVersion  变更后的资源版本（内容哈希）
 * @param releaseVersion   产出该版本的 Release 版本号，仅用于诊断
 * @param resourceRevision 变更后的服务级修订号，单调递增
 */
public record ResourceChangedEvent(@JsonProperty("serviceId") String serviceId,
                                   @JsonProperty("resourceVersion") String resourceVersion,
                                   @JsonProperty("releaseVersion") String releaseVersion,
                                   @JsonProperty("resourceRevision") long resourceRevision) {

    public static ResourceChangedEvent of(String serviceId, String resourceVersion, String releaseVersion,
                                          long resourceRevision) {
        return new ResourceChangedEvent(serviceId, resourceVersion, releaseVersion, resourceRevision);
    }

    /**
     * 幂等/检索键。
     */
    public String bizKey() {
        return "RESOURCE_CHANGED:" + serviceId + ":" + resourceRevision;
    }

    /**
     * 读模型刷新所需的引用键：{@code serviceId}（对应 {@code auth:resource:{serviceId}}）。
     */
    public String refKey() {
        return serviceId;
    }

    @Override
    public String toString() {
        return "ResourceChangedEvent{serviceId=" + serviceId + ", resourceVersion=" + resourceVersion
                + ", releaseVersion=" + releaseVersion + ", resourceRevision=" + resourceRevision + '}';
    }
}
