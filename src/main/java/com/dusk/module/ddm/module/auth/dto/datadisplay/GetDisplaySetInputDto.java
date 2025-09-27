package com.dusk.module.ddm.module.auth.dto.datadisplay;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

/**
 * 〈数据展示设置项的查询条件〉
 *
 * @author kefuming
 * @create 2022/2/8
 * @since 1.0.0
 */
@Getter
@Setter
public class GetDisplaySetInputDto extends PagedAndSortedInputDto {

    /**
     * 类型
     */
    private String displayType;
}
