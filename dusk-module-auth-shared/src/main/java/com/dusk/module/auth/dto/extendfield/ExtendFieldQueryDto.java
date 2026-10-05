package com.dusk.module.auth.dto.extendfield;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kefuming
 * 2023-04-22 10:20
 */
@Getter
@Setter
public class ExtendFieldQueryDto extends PagedAndSortedInputDto {
    private String entityClass;
    private String key;
    private List<Long> entityIdList;
    private List<String> valueList;
}
