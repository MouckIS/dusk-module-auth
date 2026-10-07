package com.dusk.module.auth.registry.entity;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.entity.CreationEntity;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * 业务权限点（Permission），见《权限优化方案-整理版》2.1 / 4.2 / 4.3。
 *
 * <p>与 {@code ApiResource} 严格分离：API 删除只让 Resource 进入 DEPRECATED，
 * Permission 与其在角色上的授权关系必须保留（红线②）。<b>代码发布不应该直接修改管理员的授权决策。</b></p>
 *
 * <p>命名说明：common-core 中已存在运行期内存模型
 * {@code com.dusk.common.core.auth.permission.Permission}（权限定义树节点），
 * 本类是该权限点在注册表中的持久化事实，故命名为 {@code PermissionDefinition} 以避免歧义。</p>
 */
@Entity
@Table(name = "sys_registry_permission")
@Getter
@Setter
@FieldNameConstants
public class PermissionDefinition extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 权限码，全局唯一，例如 {@code system:user:query}。
     */
    @Column(name = "code", nullable = false, length = 255)
    private String code;

    /**
     * 权限展示名称，用于管理后台（例如「查询用户」）。
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Permission 生命周期状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PermissionStatus status = PermissionStatus.ACTIVE;

    /**
     * 多租户侧，复用既有枚举；未在权限树中声明的权限默认视为宿主机（Host）侧，与现状一致。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "multi_tenancy_sides", length = 16)
    private MultiTenancySides multiTenancySides = MultiTenancySides.Host;

    /**
     * 首次声明该权限的服务，仅用于管理后台分组与排查，不参与授权判定。
     */
    @Column(name = "source_service_id", length = 128)
    private String sourceServiceId;

    /**
     * 最近一次进入 ORPHANED 的时刻。
     */
    @Column(name = "orphaned_at")
    private LocalDateTime orphanedAt;

    /**
     * 最近一次被管理员禁用的时刻。
     */
    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;
}
