package com.dusk.module.auth.registry.entity;

import com.dusk.common.core.entity.CreationEntity;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * 服务发布版本（Release），权限模型的第二层，见《权限优化方案-整理版》4.5 / 4.7。
 *
 * <p>Release 必须独立存在：滚动发布期间同一 Service 的多个版本会同时在线，
 * 只要旧 Release 仍为 ACTIVE，其独有 Resource 对应的 Permission 就不能进入 ORPHANED
 * （即「API 删除 ≠ 权限删除」，红线②）。</p>
 */
@Entity
@Table(name = "sys_registry_release",
        uniqueConstraints = @UniqueConstraint(name = "uk_registry_release_service_version",
                columnNames = {"service_id", "service_version"}))
@Getter
@Setter
@FieldNameConstants
public class ServiceRelease extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属服务。
     */
    @ManyToOne(targetEntity = ServiceDefinition.class, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceDefinition service;

    /**
     * 发布版本号，例如 {@code 1.2.0}。
     */
    @Column(name = "service_version", nullable = false, length = 64)
    private String serviceVersion;

    /**
     * Release 生命周期状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReleaseStatus status = ReleaseStatus.REGISTERED;

    /**
     * SDK 上报并被 Auth 接纳的 {@code resourceVersion}（API 定义版本，见 2.3）。
     *
     * <p>与 {@code serviceVersion} 严格分离：代码发布只改 serviceVersion，
     * API 定义未变时 resourceVersion 保持不变，Diff 因此可以幂等跳过。</p>
     */
    @Column(name = "resource_version", length = 128)
    private String resourceVersion;

    /**
     * 防抖起点：本 Release 开始「0 运行实例」的时刻（4.7 第一级防抖）。
     *
     * <p>有实例时置空；持续 0 实例达到阈值后 Release 才会进入 RETIRED。</p>
     */
    @Column(name = "zero_instance_since")
    private LocalDateTime zeroInstanceSince;

    /**
     * 最近一次从 Nacos 观测到该 Release 实例状态的时刻。
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    /**
     * 最近一次进入 ACTIVE 的时刻。
     */
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    /**
     * 最近一次进入 RETIRED 的时刻。
     */
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;
}
