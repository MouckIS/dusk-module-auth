package com.dusk.module.auth.registry.entity;

import com.dusk.common.core.entity.CreationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * 服务定义（Service），权限模型的第一层：{@code Service → Release → Resource}。
 *
 * <p>见《权限优化方案-整理版》2.1 / 2.2。<b>Service 只承载服务的标识信息，
 * 不承载权限事实</b>——权限事实属于 Resource 与 Permission。</p>
 *
 * <p>字段说明：{@code serviceId} 与 Nacos 注册的服务名保持一致，
 * 运行时授权 key 由它推导（红线⑫：网关 StripPrefix 之后的「服务内路径」空间）。</p>
 */
@Entity
@Table(name = "sys_registry_service")
@Getter
@Setter
@FieldNameConstants
public class ServiceDefinition extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 服务唯一标识，与 Nacos 服务名一致，例如 {@code dusk-module-user}。
     */
    @Column(name = "service_id", nullable = false, length = 128)
    private String serviceId;

    /**
     * 展示名称，仅用于管理后台可读性。
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * 部署系统标记的「当前默认 Release 版本」，用于 5.7 的多版本冲突仲裁。
     *
     * <p>为空时由 Auth 侧回退为「最高版本号的 ACTIVE Release」。</p>
     */
    @Column(name = "default_release_version", length = 64)
    private String defaultReleaseVersion;

    /**
     * 服务级资源读模型修订号，见《权限优化方案-整理版》3.2。
     *
     * <p>单调递增，只在「影响该服务有效资源并集的变更」发生时推进：
     * Release 注册、换绑审批、Release 状态变化等。Redis 投影与 MQ 事件都按它排序，
     * 保证旧快照、旧事件不会覆盖新读模型。{@code resourceVersion} 是内容哈希，不参与排序（红线⑨）。</p>
     */
    @Column(name = "resource_revision", nullable = false)
    private long resourceRevision;

    /**
     * 推进修订号并返回新值。
     */
    public long nextResourceRevision() {
        resourceRevision = resourceRevision + 1;
        return resourceRevision;
    }
}
