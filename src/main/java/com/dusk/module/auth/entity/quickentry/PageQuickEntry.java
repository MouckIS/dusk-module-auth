package com.dusk.module.auth.entity.quickentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 〈页面的快捷入口〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "sys_page_quick_entry")
@FieldNameConstants
public class PageQuickEntry extends FullAuditedEntity {

    @Schema(description = "前端路由的name")
    private String routeName;
}
