package com.dusk.module.auth.dto.todo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-08-05 9:43
 */
@Getter
@Setter
public class GetTodosInput extends PagedAndSortedInputDto {
    @ApiModelProperty("类型名称模糊查询")
    private String typeName;

    @ApiModelProperty("类型标题模糊查询")
    private String title;

    @ApiModelProperty("待办类型编码，in 查询，也就是满足其一即可")
    private List<String> type;
}
