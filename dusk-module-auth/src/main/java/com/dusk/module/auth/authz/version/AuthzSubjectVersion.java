package com.dusk.module.auth.authz.version;

import com.dusk.common.core.auth.registry.AuthzScope;
import com.dusk.common.core.entity.CreationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * 用户代际版本，见《权限优化方案-整理版》3.2 / 5.3。
 *
 * <p>修改 UserRole、禁用/启用用户、强制下线、修改令牌安全代际时递增。
 * JWT 携带签发时的 {@code subjectVersion}，网关据此拒绝旧令牌或要求回源追平。</p>
 */
@Entity
@Table(name = "sys_authz_subject_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_authz_subject_version_scope_user",
                columnNames = {"tenant_scope", "user_id"}))
@Getter
@Setter
@FieldNameConstants
public class AuthzSubjectVersion extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 授权作用域：{@code HOST} 或 {@code TENANT:<id>}。
     */
    @Column(name = "tenant_scope", nullable = false, length = 64)
    private String tenantScope = AuthzScope.HOST;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subject_version", nullable = false)
    private long subjectVersion;
}
