package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.module.auth.enums.UserPrintType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;

/**
 *
 * @author caiwenjun
 * @date 2024/1/5 11:07
 */
@Data
@ApiModel(value = "UsersPrintInput", description = "用户信息打印实体类")
public class UsersPrintInput {
    @ApiModelProperty("人员id")
    public List<Long> userIds;

    @Enumerated(EnumType.STRING)
    @ApiModelProperty("人员打印类型")
    public UserPrintType printType = UserPrintType.INNER;

    @ApiModelProperty("项目名称")
    public String projectName;

    @ApiModelProperty("工种")
    public String workType;

}
