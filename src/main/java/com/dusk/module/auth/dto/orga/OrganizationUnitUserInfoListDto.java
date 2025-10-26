package com.dusk.module.auth.dto.orga;

import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitUserListDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kefuming
 * @CreateTime 2022-10-27
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUnitUserInfoListDto extends OrganizationUnitUserListDto {
    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "账号")
    private String userName;

    @Schema(description = "邮箱地址")
    private String emailAddress;

    @Schema(description = "所属组织机构id")
    private Long organizationUnitId;

    @Schema(description = "所属组织机构名称")
    private String organizationUnitName;

    @Schema(description = "工作岗位")
    private String job;

    @Schema(description = "手机号")
    private String phoneNo;

    @Schema(description = "用户状态")
    private UserStatus userStatus;

    public String getStatusName() {
        return userStatus == null ? "" : userStatus.getDisplayName();
    }
}
