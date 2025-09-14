package com.dusk.module.auth.entity.datadisplay;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.FullAuditedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 〈首页数据展示的设置项〉
 *
 * @author kefuming
 * @create 2022/2/8
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "sys_data_display_set")
@FieldNameConstants
public class DataDisplaySet extends FullAuditedEntity {

    /**
     * 类型
     */
    private String displayType;
}
