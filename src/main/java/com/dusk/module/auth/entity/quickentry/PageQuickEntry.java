package com.dusk.module.auth.entity.quickentry;

import com.dusk.common.core.entity.FullAuditedEntity;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

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
