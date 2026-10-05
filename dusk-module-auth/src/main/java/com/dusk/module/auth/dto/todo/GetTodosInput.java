package com.dusk.module.auth.dto.todo;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-08-05 9:43
 */
@Getter
@Setter
public class GetTodosInput extends PagedAndSortedInputDto {
    @Schema(description = "类型名称模糊查询")
    private String typeName;

    @Schema(description = "类型标题模糊查询")
    private String title;

    @Schema(description = "待办类型编码，in 查询，也就是满足其一即可")
    private List<String> type;
}
