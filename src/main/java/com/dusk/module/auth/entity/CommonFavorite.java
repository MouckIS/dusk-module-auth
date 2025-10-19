package com.dusk.module.auth.entity;

import com.dusk.common.core.annotation.DataPermission;
import com.dusk.common.core.entity.FullAuditedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * 公用收藏夹
 *
 * @author chenzhi1
 * @date 2021/5/24 17:07
 */
@Entity
@Table(name = "common_favorite")
@Getter
@Setter
@FieldNameConstants
public class CommonFavorite extends FullAuditedEntity {
    /**
     * 收藏名字
     */
    private String name;
    /**
     * 收藏类型
     */
    private String type;
    /**
     * 收藏内容
     */
    private String content;
    /**
     * 组织机构
     */
    @DataPermission
    private Long orgId;

    /**
     * 是否公开
     */
    private Boolean isPublic;
}
