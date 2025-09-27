package com.dusk.module.ddm.module.auth.entity.datadisplay;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

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
