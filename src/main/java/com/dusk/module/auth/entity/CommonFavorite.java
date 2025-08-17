package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.annotation.DataPermission;
import com.dusk.common.framework.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 公用收藏夹
 *
 * @Author chenzhi1
 * @Date 2021/5/24 17:07
 */
@Entity
@Table(name = "common_favorite")
@Data
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
