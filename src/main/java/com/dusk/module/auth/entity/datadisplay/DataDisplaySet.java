package com.dusk.module.auth.entity.datadisplay;

import com.dusk.common.core.entity.FullAuditedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * 〈首页数据展示的设置项〉
 *
 * @author kefuming
 * @create 2022/2/8
 * @since 1.0.0
 */
@Getter
@Setter
@Entity
@Table(name = "sys_data_display_set")
@FieldNameConstants
public class DataDisplaySet extends FullAuditedEntity {

    /**
     * 类型
     */
    private String displayType;
}
