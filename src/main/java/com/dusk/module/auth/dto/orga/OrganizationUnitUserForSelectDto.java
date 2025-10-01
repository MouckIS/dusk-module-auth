package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.dto.EntityDto;

/**
 * 组织机构下拉选择用户的Dto
 *
 * @author kefuming
 * @date 2020/10/16 16:11
 */
@Data
@NoArgsConstructor
@FieldNameConstants
public class OrganizationUnitUserForSelectDto {

    @ApiModelProperty("用户id")
    private Long id;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("账号")
    private String userName;

    public OrganizationUnitUserForSelectDto(Long id, String name, String userName) {
        this.id = id;
        this.name = name;
        this.userName = userName;
    }


}
