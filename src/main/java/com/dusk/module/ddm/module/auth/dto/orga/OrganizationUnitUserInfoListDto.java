package com.dusk.module.ddm.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.module.auth.enums.UserStatus;

/**
 * @author kefuming
 * @CreateTime 2022-10-27
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUnitUserInfoListDto extends OrganizationUnitUserListDto {
    @ApiModelProperty("用户id")
    private Long id;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("账号")
    private String userName;

    @ApiModelProperty("邮箱地址")
    private String emailAddress;

    @ApiModelProperty("所属组织机构id")
    private Long organizationUnitId;

    @ApiModelProperty("所属组织机构名称")
    private String organizationUnitName;

    @ApiModelProperty("工作岗位")
    private String job;

    @ApiModelProperty("手机号")
    private String phoneNo;

    @ApiModelProperty("用户状态")
    private UserStatus userStatus;

    public String getStatusName() {
        return userStatus == null ? "" : userStatus.getDisplayName();
    }
}
