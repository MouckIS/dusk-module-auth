package com.dusk.module.auth.entity.quickentry;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 〈页面的快捷入口〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
@Getter
@Setter
@Entity
@Table(name = "sys_page_quick_entry")
@FieldNameConstants
public class PageQuickEntry extends FullAuditedEntity {

    @ApiModelProperty("前端路由的name")
    private String routeName;
}
