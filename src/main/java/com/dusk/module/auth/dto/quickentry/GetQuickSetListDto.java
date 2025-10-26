package com.dusk.module.auth.dto.quickentry;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
@Getter
@Setter
public class GetQuickSetListDto extends PagedAndSortedInputDto {

    @Schema(description = "前端路由的name")
    private String routeName;
}
