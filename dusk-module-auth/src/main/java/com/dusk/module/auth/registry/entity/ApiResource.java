package com.dusk.module.auth.registry.entity;

import com.dusk.common.core.entity.CreationEntity;
import com.dusk.module.auth.registry.enums.ResourceStatus;
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
 * API 资源（Resource），权限模型的第三层，见《权限优化方案-整理版》2.1 / 4.1 / 4.9。
 *
 * <p>一个 Resource 是一个 HTTP API，绑定到一个业务权限点。红线①：
 * <b>Permission ≠ Resource</b>；红线④：<b>Resource 必须绑定 Release</b>。</p>
 *
 * <p>换绑语义（4.9）：当快照把同一 {@code (method, path)} 的 permission 从旧值改为新值时，
 * {@link #permissionCode} 保持旧值不动（运行时继续按旧绑定执行），
 * 新值写入 {@link #pendingPermissionCode} 并挂起等待管理员确认。</p>
 */
@Entity
@Table(name = "sys_registry_resource",
        uniqueConstraints = @UniqueConstraint(name = "uk_registry_resource_release_method_path",
                columnNames = {"release_id", "http_method", "path"}))
@Getter
@Setter
@FieldNameConstants
public class ApiResource extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属发布版本。
     */
    @ManyToOne(targetEntity = ServiceRelease.class, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id", nullable = false)
    private ServiceRelease release;

    /**
     * SDK 生成的资源标识（用于跨快照追踪同一资源，非数据库主键）。
     */
    @Column(name = "resource_id", length = 128)
    private String resourceId;

    /**
     * HTTP 方法，统一大写存储。
     */
    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    /**
     * 服务内路径（不含网关 {@code /api/{serviceId}} 前缀），红线⑫。
     */
    @Column(name = "path", nullable = false, length = 512)
    private String path;

    /**
     * 当前生效的权限码，也是运行时授权实际使用的绑定。
     */
    @Column(name = "permission_code", length = 255)
    private String permissionCode;

    /**
     * 待确认的权限码：仅在 {@link ResourceStatus#PENDING_REBIND} 与
     * {@link ResourceStatus#REBIND_REJECTED} 下非空。
     *
     * <p>被拒绝后仍然保留该值，用于识别「下次快照仍上报同一新权限 → 重新告警」（4.9）。</p>
     */
    @Column(name = "pending_permission_code", length = 255)
    private String pendingPermissionCode;

    /**
     * 资源生命周期状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ResourceStatus status = ResourceStatus.ACTIVE;

    /**
     * 最近一次状态变更时刻。
     */
    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;
}
