package com.dusk.module.auth.dto.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * 组织机构下拉选择用户的Dto
 *
 * @author kefuming
 * @date 2022/09/23 16:32
 */
@Data
@NoArgsConstructor
@FieldNameConstants
public class StationUserDto {

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "账号")
    private String userName;

    public StationUserDto(Long id, String name, String userName) {
        this.id = id;
        this.name = name;
        this.userName = userName;
    }


}
