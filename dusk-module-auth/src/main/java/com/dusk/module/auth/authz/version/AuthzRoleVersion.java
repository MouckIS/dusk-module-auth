package com.dusk.module.auth.authz.version;

import com.dusk.common.core.entity.CreationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * 角色权限版本号，见《权限优化方案-整理版》2.3 / 8.5。
 *
 * <p>{@code permissionVersion} 是「角色授权集合」的单调递增版本，用于网关 L1 缓存的
 * <b>精确失效</b>：事件版本比缓存版本更新才失效。因此它必须满足两个性质：</p>
 * <ol>
 *   <li><b>按 role 单调递增</b>——否则乱序/重复事件会把新数据换成旧数据，或该失效而不失效；</li>
 *   <li><b>持久化</b>——版本号在进程重启后仍须可用，否则重启后事件版本可能与缓存版本不可比。</li>
 * </ol>
 *
 * <p>与 {@code resourceVersion} 的差别（红线⑩）：后者是内容哈希、只有相等/不等语义；
 * 前者是整数、有大小关系。二者不可互换。</p>
 */
@Entity
@Table(name = "sys_authz_role_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_authz_role_version_role_id", columnNames = "role_id"))
@Getter
@Setter
@FieldNameConstants
public class AuthzRoleVersion extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID。宿主/租户角色共用一张表，故不按租户唯一。
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * 角色所属租户，冗余保存仅用于排障（失效判定以 roleId 为准）。
     */
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * 当前权限版本号，单调递增，从 1 开始。
     */
    @Column(name = "permission_version", nullable = false)
    private long permissionVersion;
}
