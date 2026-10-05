package com.dusk.module.auth.dto.extendfield;

import com.dusk.common.core.dto.EntityDto;
import lombok.Getter;
import lombok.Setter;

/**
 * @author 王斯博
 * 2023-04-21 16:28
 */
@Getter
@Setter
public class ExtendFieldDto extends EntityDto {
    /**
     * 实体id
     */
    private Long entityId;
    /**
     * 实体类名称
     */
    private String entityClass;

    private String key;

    private String value;
}
